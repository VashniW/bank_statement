package com.bank.statements.service.balance;

import com.bank.statements.model.Account;
import com.bank.statements.model.Statement;
import com.bank.statements.model.Transaction;
import com.bank.statements.repository.StatementRepository;
import com.bank.statements.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * Shared building block for anything that needs "opening balance for a
 * period" or "day-by-day balance across a period" - used by both the fee
 * engine (min balance / overdraft rules) and the interest engine (daily
 * balance method). Keeping this in one place means both always agree on
 * what the balance was on any given day.
 */
@Component
@RequiredArgsConstructor
public class PeriodBalanceService {

    private final StatementRepository statementRepository;
    private final TransactionRepository transactionRepository;
    private final DailyBalanceCalculator dailyBalanceCalculator;

    /**
     * Opening balance = previous period's closing balance if one exists,
     * else the account's own recorded opening balance (which may be
     * non-zero for an onboarded/migrated account whose real starting
     * balance predates our transaction history - NOT a hardcoded zero).
     */
    public BigDecimal getOpeningBalance(Account account, YearMonth period) {
        return statementRepository
                .findMostRecentPrevious(account.getId(), period.getYear(), period.getMonthValue())
                .map(Statement::getClosingBalance)
                .orElseGet(() -> account.getOpeningBalance() != null
                        ? account.getOpeningBalance()
                        : BigDecimal.ZERO);
    }

    public List<Transaction> getTransactions(Account account, YearMonth period) {
        return transactionRepository.findAllByAccountIdAndTxnDateBetweenOrderByTxnDateAsc(
                account.getId(), period.atDay(1), period.atEndOfMonth());
    }

    public List<DailyBalance> getDailyBalances(Account account, YearMonth period) {
        BigDecimal openingBalance = getOpeningBalance(account, period);
        List<Transaction> transactions = getTransactions(account, period);
        return dailyBalanceCalculator.calculate(
                openingBalance, period.atDay(1), period.atEndOfMonth(), transactions);
    }
}