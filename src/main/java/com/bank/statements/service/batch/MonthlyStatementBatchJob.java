package com.bank.statements.service.batch;

import com.bank.statements.model.Account;
import com.bank.statements.model.AccountStatus;
import com.bank.statements.repository.AccountRepository;
import com.bank.statements.service.statement.StatementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs statement generation for every active account, for the previous
 * calendar month.
 *
 * NOTE: this calls StatementService.getOrGenerateFinalStatement() only -
 * that single method already handles fees -> interest -> statement
 * generation internally, plus the account-level lock and the recursive
 * "ensure prior periods exist" chain. Calling fee/interest generation
 * separately here (as an earlier version of this class did) would run that
 * work OUTSIDE the lock and duplicate what getOrGenerateFinalStatement
 * already does - so this stays a single call per account.
 *
 * Each account is processed independently - if one account fails (bad data,
 * unexpected exception, etc.), it's logged and the batch continues with the
 * next account rather than aborting the whole run.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MonthlyStatementBatchJob {

    private final AccountRepository accountRepository;
    private final StatementService statementService;

    /** Runs automatically on the schedule configured in statement.batch.cron. */
    @Scheduled(cron = "${statement.batch.cron}")
    public void runScheduled() {
        YearMonth previousMonth = YearMonth.now().minusMonths(1);
        log.info("Scheduled batch trigger firing for period {}", previousMonth);
        runForPeriod(previousMonth);
    }

    /**
     * Runs generation for a specific period. Exposed as a public method so
     * it can also be triggered manually (admin endpoint) for backfills or
     * re-runs.
     *
     * IMPORTANT: rejects the current or any future period. Statement
     * generation for those must go through StatementService.getStatement(),
     * which produces a live PROVISIONAL view instead of a persisted final
     * one. Generating a "final" GENERATED statement (and permanently
     * locking in fees/interest) for a month that hasn't finished yet would
     * be based on incomplete data and can't be undone through normal flow
     * afterward - so this is enforced here too, not just at the API layer.
     */
    public BatchRunSummary runForPeriod(YearMonth period) {
        YearMonth currentMonth = YearMonth.now();
        if (!period.isBefore(currentMonth)) {
            throw new IllegalArgumentException(
                    "Cannot run final statement generation for the current or a future period: "
                            + period + ". Only completed months can be finalized.");
        }

        List<Account> accounts = accountRepository.findAllByStatus(AccountStatus.ACTIVE);
        List<String> failureDetails = new ArrayList<>();
        int succeeded = 0;

        log.info("Starting monthly statement batch for period {} - {} active accounts",
                period, accounts.size());

        for (Account account : accounts) {
            try {
                statementService.getOrGenerateFinalStatement(account, period);
                succeeded++;
            } catch (Exception e) {
                String detail = "Account " + account.getId() + " (" + account.getAccountNumber()
                        + "): " + e.getMessage();
                failureDetails.add(detail);
                log.error("Batch generation failed for account {} period {}",
                        account.getId(), period, e);
                // Deliberately continue - one account's failure must not block the rest.
            }
        }

        int failed = failureDetails.size();
        log.info("Batch complete for period {}: {} succeeded, {} failed out of {} accounts",
                period, succeeded, failed, accounts.size());

        return new BatchRunSummary(period, accounts.size(), succeeded, failed, failureDetails);
    }
}
