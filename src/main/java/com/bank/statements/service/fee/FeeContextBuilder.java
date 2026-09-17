package com.bank.statements.service.fee;

import com.bank.statements.model.Account;
import com.bank.statements.model.Transaction;
import com.bank.statements.model.TransactionType;
import com.bank.statements.service.balance.DailyBalance;
import com.bank.statements.service.balance.PeriodBalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FeeContextBuilder {

    private final PeriodBalanceService periodBalanceService;

    public FeeContext build(Account account, YearMonth period) {
        BigDecimal openingBalance = periodBalanceService.getOpeningBalance(account, period);
        List<Transaction> transactions = periodBalanceService.getTransactions(account, period);
        List<DailyBalance> dailyBalances = periodBalanceService.getDailyBalances(account, period);

        BigDecimal minimumDailyBalance = dailyBalances.stream()
                .map(DailyBalance::balance)
                .min(Comparator.naturalOrder())
                .orElse(openingBalance);

        BigDecimal averageDailyBalance = dailyBalances.isEmpty()
                ? openingBalance
                : dailyBalances.stream()
                        .map(DailyBalance::balance)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(dailyBalances.size()), 2, RoundingMode.HALF_UP);

        boolean hadNegativeBalanceDay = dailyBalances.stream()
                .anyMatch(d -> d.balance().compareTo(BigDecimal.ZERO) < 0);

        long atmWithdrawalCount = transactions.stream()
                .filter(t -> t.getType() == TransactionType.DEBIT)
                .filter(t -> "ATM".equalsIgnoreCase(t.getCategory()))
                .count();

        return FeeContext.builder()
                .account(account)
                .period(period)
                .openingBalance(openingBalance)
                .transactions(transactions)
                .dailyBalances(dailyBalances)
                .averageDailyBalance(averageDailyBalance)
                .minimumDailyBalance(minimumDailyBalance)
                .hadNegativeBalanceDay(hadNegativeBalanceDay)
                .atmWithdrawalCount(atmWithdrawalCount)
                .build();
    }
}
