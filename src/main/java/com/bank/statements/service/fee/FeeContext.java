package com.bank.statements.service.fee;

import com.bank.statements.model.Account;
import com.bank.statements.model.Transaction;
import com.bank.statements.service.balance.DailyBalance;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * Precomputed data for one account + period, built once by FeeContextBuilder
 * and passed to every FeeRule - avoids each rule re-querying the database
 * independently.
 */
@Getter
@Builder
public class FeeContext {

    private final Account account;
    private final YearMonth period;
    private final BigDecimal openingBalance;
    private final List<Transaction> transactions;
    private final List<DailyBalance> dailyBalances;

    private final BigDecimal averageDailyBalance;
    private final BigDecimal minimumDailyBalance;
    private final boolean hadNegativeBalanceDay;
    private final long atmWithdrawalCount;
}
