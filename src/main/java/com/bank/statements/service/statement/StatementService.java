package com.bank.statements.service.statement;

import com.bank.statements.model.*;
import com.bank.statements.repository.AccountRepository;
import com.bank.statements.repository.FeeRepository;
import com.bank.statements.repository.InterestAccrualRepository;
import com.bank.statements.repository.StatementLineRepository;
import com.bank.statements.repository.StatementRepository;
import com.bank.statements.repository.TransactionRepository;
import com.bank.statements.service.balance.PeriodBalanceService;
import com.bank.statements.service.fee.FeeGenerationService;
import com.bank.statements.service.interest.InterestGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Builds the final monthly statement by combining transactions, fees, and
 * interest for a period.
 *
 * Two distinct paths:
 *  - Completed month (before the current calendar month): generated once,
 *    persisted permanently as an immutable GENERATED statement, and served
 *    from cache on every subsequent request for that period.
 *  - Current, in-progress month: cannot be final since more activity may
 *    still occur before month-end, and fees/interest aren't fully accrued
 *    yet. Computed live on every request, covering the 1st of the month
 *    through today only, marked PROVISIONAL, and never persisted.
 *
 * CORRECTNESS GUARANTEE: getOrGenerateFinalStatement() ensures every prior
 * period back to the account's first activity is generated (in order)
 * before generating the requested period, so opening balances are always
 * correct even if statements were never generated in strict chronological
 * order before now (e.g. a user jumps straight to viewing October before
 * anyone ever generated September). This recursion is bounded by the
 * account's actual transaction history, not open-ended.
 *
 * CONCURRENCY: a pessimistic row lock on the Account is acquired before any
 * generation work, so two concurrent requests for the same account (e.g. a
 * batch run and an on-demand view happening at the same time) serialize
 * instead of racing against the fee/interest/statement unique constraints.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StatementService {

    private final AccountRepository accountRepository;
    private final StatementRepository statementRepository;
    private final StatementLineRepository statementLineRepository;
    private final FeeRepository feeRepository;
    private final InterestAccrualRepository interestAccrualRepository;
    private final TransactionRepository transactionRepository;
    private final PeriodBalanceService periodBalanceService;
    private final FeeGenerationService feeGenerationService;
    private final InterestGenerationService interestGenerationService;

    /** One line item before it's turned into a persisted/transient StatementLine. */
    private record RawLine(LocalDate date, String description, BigDecimal amount,
                            StatementLineSourceType sourceType, TransactionType dcIndicator) {
    }

    @Transactional
    public StatementResult getStatement(Account account, YearMonth period) {
        YearMonth currentMonth = YearMonth.now();

        if (period.isAfter(currentMonth)) {
            throw new IllegalArgumentException(
                    "Cannot request a statement for a future period: " + period);
        }

        YearMonth openingMonth = getAccountOpeningMonth(account);
        if (period.isBefore(openingMonth)) {
            throw new IllegalArgumentException(
                    "Cannot request a statement for period " + period + " before account was opened: " + openingMonth);
        }

        if (period.equals(currentMonth)) {
            accountRepository.lockForUpdate(account.getId());
            ensurePriorChainGenerated(account, period);
            return buildProvisionalStatement(account, period);
        }

        return getOrGenerateFinalStatement(account, period);
    }

    // ---------------------------------------------------------------
    // Completed month - cached, immutable once generated
    // ---------------------------------------------------------------

    @Transactional
    public StatementResult getOrGenerateFinalStatement(Account account, YearMonth period) {
        // Serialize concurrent generation attempts for this account. Held for
        // the rest of this transaction, including any recursive calls below.
        accountRepository.lockForUpdate(account.getId());

        ensurePriorChainGenerated(account, period);

        BigDecimal expectedOpeningBalance = periodBalanceService.getOpeningBalance(account, period);

        Optional<Statement> existing = statementRepository
                .findByAccountIdAndPeriodYearAndPeriodMonth(
                        account.getId(), period.getYear(), period.getMonthValue());

        if (existing.isPresent()) {
            Statement statement = existing.get();
            // Invariant: statement opening balance must match the closing balance of the previous period
            if (statement.getOpeningBalance().compareTo(expectedOpeningBalance) == 0) {
                List<StatementLine> lines =
                        statementLineRepository.findAllByStatementIdOrderByLineDateAsc(statement.getId());
                return new StatementResult(statement, lines);
            }

            log.info("Statement for account {} period {} has stale opening balance (found {}, expected {}). Re-generating...",
                    account.getId(), period, statement.getOpeningBalance(), expectedOpeningBalance);

            if (statement.getPdfPath() != null) {
                try {
                    Files.deleteIfExists(Path.of(statement.getPdfPath()));
                } catch (Exception e) {
                    log.warn("Could not delete stale cached PDF: {}", statement.getPdfPath(), e);
                }
                statement.setPdfPath(null);
            }

            feeRepository.deleteAllByAccountIdAndPeriodYearAndPeriodMonth(
                    account.getId(), period.getYear(), period.getMonthValue());
            interestAccrualRepository.deleteByAccountIdAndPeriodYearAndPeriodMonth(
                    account.getId(), period.getYear(), period.getMonthValue());
            statementLineRepository.deleteAllByStatementId(statement.getId());

            return generateOrUpdateStatement(statement, account, period, expectedOpeningBalance);
        }

        return generateOrUpdateStatement(null, account, period, expectedOpeningBalance);
    }

    /**
     * Ensures the immediately preceding period's statement exists before we
     * compute this period's opening balance from it - recursing back as far
     * as needed down to the account's opened month.
     */
    private void ensurePriorChainGenerated(Account account, YearMonth period) {
        YearMonth floor = getAccountOpeningMonth(account);
        YearMonth previousPeriod = period.minusMonths(1);

        if (previousPeriod.isBefore(floor)) {
            return; // reached account opened month floor - nothing to chain prior
        }

        log.debug("Ensuring prior statement chain back to {} for account {} before generating {}",
                previousPeriod, account.getId(), period);

        getOrGenerateFinalStatement(account, previousPeriod);
    }

    private YearMonth getAccountOpeningMonth(Account account) {
        if (account.getOpenedDate() != null) {
            return YearMonth.from(account.getOpenedDate());
        }
        return YearMonth.of(2026, 1);
    }

    private StatementResult generateOrUpdateStatement(Statement existing, Account account, YearMonth period, BigDecimal openingBalance) {
        // Fees and interest must be posted before the statement aggregates them.
        feeGenerationService.generateFeesForPeriod(account, period);
        interestGenerationService.generateInterestForPeriod(account, period);

        LocalDate periodEnd = period.atEndOfMonth();

        List<Transaction> transactions = periodBalanceService.getTransactions(account, period);
        List<Fee> fees = feeRepository.findAllByAccountIdAndPeriodYearAndPeriodMonth(
                account.getId(), period.getYear(), period.getMonthValue());
        InterestAccrual interest = interestAccrualRepository
                .findByAccountIdAndPeriodYearAndPeriodMonth(
                        account.getId(), period.getYear(), period.getMonthValue())
                .orElse(null);

        BigDecimal totalDebits = sumByType(transactions, TransactionType.DEBIT);
        BigDecimal totalCredits = sumByType(transactions, TransactionType.CREDIT);
        BigDecimal totalFees = fees.stream().map(Fee::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalInterest = interest != null ? interest.getAmount() : BigDecimal.ZERO;

        BigDecimal closingBalance = openingBalance.add(totalCredits).subtract(totalDebits).subtract(totalFees);
        if (interest != null) {
            closingBalance = interest.getDirection() == InterestDirection.EARNED
                    ? closingBalance.add(totalInterest)
                    : closingBalance.subtract(totalInterest);
        }

        Statement statement;
        if (existing == null) {
            statement = Statement.builder()
                    .account(account)
                    .periodYear(period.getYear())
                    .periodMonth(period.getMonthValue())
                    .periodEndDate(periodEnd)
                    .openingBalance(openingBalance)
                    .closingBalance(closingBalance)
                    .totalDebits(totalDebits)
                    .totalCredits(totalCredits)
                    .totalFees(totalFees)
                    .totalInterest(totalInterest)
                    .generatedAt(LocalDateTime.now())
                    .status(StatementStatus.GENERATED)
                    .build();
        } else {
            existing.setPeriodEndDate(periodEnd);
            existing.setOpeningBalance(openingBalance);
            existing.setClosingBalance(closingBalance);
            existing.setTotalDebits(totalDebits);
            existing.setTotalCredits(totalCredits);
            existing.setTotalFees(totalFees);
            existing.setTotalInterest(totalInterest);
            existing.setGeneratedAt(LocalDateTime.now());
            existing.setStatus(StatementStatus.GENERATED);
            statement = existing;
        }

        statement = statementRepository.save(statement);

        List<RawLine> rawLines = buildRawLines(transactions, fees, interest);
        List<StatementLine> lines = materializeLines(statement, openingBalance, rawLines);
        statementLineRepository.saveAll(lines);

        log.info("{} final statement for account {} period {}: opening={} closing={}",
                existing == null ? "Generated" : "Updated", account.getId(), period, openingBalance, closingBalance);

        propagateForwardIfNextExists(account, period, closingBalance);

        return new StatementResult(statement, lines);
    }

    private void propagateForwardIfNextExists(Account account, YearMonth period, BigDecimal newClosingBalance) {
        YearMonth nextPeriod = period.plusMonths(1);
        if (!nextPeriod.isBefore(YearMonth.now())) {
            return; // don't finalize current or future months
        }

        Optional<Statement> nextExisting = statementRepository
                .findByAccountIdAndPeriodYearAndPeriodMonth(
                        account.getId(), nextPeriod.getYear(), nextPeriod.getMonthValue());

        if (nextExisting.isPresent()) {
            Statement nextStmt = nextExisting.get();
            if (nextStmt.getOpeningBalance().compareTo(newClosingBalance) != 0) {
                log.info("Propagating closing balance change from {} ({}) to {} for account {}",
                        period, newClosingBalance, nextPeriod, account.getId());
                getOrGenerateFinalStatement(account, nextPeriod);
            }
        }
    }

    // ---------------------------------------------------------------
    // Current, in-progress month - live, never persisted
    // ---------------------------------------------------------------

    private StatementResult buildProvisionalStatement(Account account, YearMonth period) {
        LocalDate today = LocalDate.now();

        BigDecimal openingBalance = periodBalanceService.getOpeningBalance(account, period);
        List<Transaction> transactionsTillToday = periodBalanceService.getTransactions(account, period)
                .stream()
                .filter(t -> !t.getTxnDate().isAfter(today))
                .toList();

        // Fees and interest for an in-progress month aren't final - a full month's
        // worth of daily balances is needed for interest, and fee rules (e.g.
        // average balance) aren't meaningful on a partial period. Shown as zero
        // until the month completes and the final statement is generated.
        BigDecimal totalDebits = sumByType(transactionsTillToday, TransactionType.DEBIT);
        BigDecimal totalCredits = sumByType(transactionsTillToday, TransactionType.CREDIT);
        BigDecimal totalFees = BigDecimal.ZERO;
        BigDecimal totalInterest = BigDecimal.ZERO;

        BigDecimal closingBalance = openingBalance.add(totalCredits).subtract(totalDebits);

        Statement statement = Statement.builder()
                .account(account)
                .periodYear(period.getYear())
                .periodMonth(period.getMonthValue())
                .periodEndDate(today)
                .openingBalance(openingBalance)
                .closingBalance(closingBalance)
                .totalDebits(totalDebits)
                .totalCredits(totalCredits)
                .totalFees(totalFees)
                .totalInterest(totalInterest)
                .generatedAt(LocalDateTime.now())
                .status(StatementStatus.PROVISIONAL)
                .build();
        // NOT saved - statement.getId() remains null, this is a live view only

        List<RawLine> rawLines = buildRawLines(transactionsTillToday, List.of(), null);
        List<StatementLine> lines = materializeLines(null, openingBalance, rawLines);
        // lines are NOT saved either

        return new StatementResult(statement, lines);
    }

    // ---------------------------------------------------------------
    // Shared helpers
    // ---------------------------------------------------------------

    private BigDecimal sumByType(List<Transaction> transactions, TransactionType type) {
        return transactions.stream()
                .filter(t -> t.getType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<RawLine> buildRawLines(List<Transaction> transactions, List<Fee> fees, InterestAccrual interest) {
        List<RawLine> rawLines = new ArrayList<>();

        for (Transaction t : transactions) {
            rawLines.add(new RawLine(t.getTxnDate(), t.getDescription(), t.getAmount(),
                    StatementLineSourceType.TRANSACTION, t.getType()));
        }
        for (Fee f : fees) {
            rawLines.add(new RawLine(f.getFeeDate(), f.getDescription(), f.getAmount(),
                    StatementLineSourceType.FEE, TransactionType.DEBIT));
        }
        if (interest != null) {
            TransactionType dc = interest.getDirection() == InterestDirection.EARNED
                    ? TransactionType.CREDIT : TransactionType.DEBIT;
            rawLines.add(new RawLine(interest.getAccrualDate(), interest.getDescription(),
                    interest.getAmount(), StatementLineSourceType.INTEREST, dc));
        }

        rawLines.sort(Comparator.comparing(RawLine::date));
        return rawLines;
    }

    /** Replays raw lines in date order to compute a running balance_after per line. */
    private List<StatementLine> materializeLines(Statement statement, BigDecimal openingBalance,
                                                  List<RawLine> rawLines) {
        List<StatementLine> lines = new ArrayList<>();
        BigDecimal runningBalance = openingBalance;

        for (RawLine raw : rawLines) {
            runningBalance = raw.dcIndicator() == TransactionType.CREDIT
                    ? runningBalance.add(raw.amount())
                    : runningBalance.subtract(raw.amount());

            lines.add(StatementLine.builder()
                    .statement(statement)
                    .lineDate(raw.date())
                    .description(raw.description())
                    .amount(raw.amount())
                    .sourceType(raw.sourceType())
                    .dcIndicator(raw.dcIndicator())
                    .balanceAfter(runningBalance)
                    .build());
        }
        return lines;
    }
}
