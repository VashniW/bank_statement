package com.bank.statements.service.balance;

import com.bank.statements.model.Transaction;
import com.bank.statements.model.TransactionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Walks day-by-day through a period, applying that day's transactions to a
 * running balance, producing one DailyBalance entry per calendar day in the
 * range [periodStart, periodEnd] inclusive.
 *
 * This is the shared building block behind:
 *  - Fee rules that depend on daily/average balance (min balance, overdraft)
 *  - Interest calculation (daily-balance method, Phase 3)
 */
@Component
public class DailyBalanceCalculator {

    public List<DailyBalance> calculate(BigDecimal openingBalance,
                                         LocalDate periodStart,
                                         LocalDate periodEnd,
                                         List<Transaction> transactionsInPeriod) {

        Map<LocalDate, List<Transaction>> byDate = transactionsInPeriod.stream()
                .collect(Collectors.groupingBy(Transaction::getTxnDate));

        List<DailyBalance> result = new ArrayList<>();
        BigDecimal runningBalance = openingBalance;

        for (LocalDate date = periodStart; !date.isAfter(periodEnd); date = date.plusDays(1)) {
            for (Transaction t : byDate.getOrDefault(date, List.of())) {
                runningBalance = t.getType() == TransactionType.CREDIT
                        ? runningBalance.add(t.getAmount())
                        : runningBalance.subtract(t.getAmount());
            }
            result.add(new DailyBalance(date, runningBalance));
        }
        return result;
    }
}
