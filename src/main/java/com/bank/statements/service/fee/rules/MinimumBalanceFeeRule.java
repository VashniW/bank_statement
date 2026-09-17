package com.bank.statements.service.fee.rules;

import com.bank.statements.config.FeeProperties;
import com.bank.statements.model.Account;
import com.bank.statements.model.AccountType;
import com.bank.statements.model.FeeType;
import com.bank.statements.service.fee.FeeContext;
import com.bank.statements.service.fee.FeeDecision;
import com.bank.statements.service.fee.FeeRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.Optional;

/**
 * Charges a flat fee if the account's average daily balance for the month
 * fell below the configured minimum threshold. Applies to SAVINGS accounts.
 */
@Component
@RequiredArgsConstructor
public class MinimumBalanceFeeRule implements FeeRule {

    private final FeeProperties feeProperties;

    @Override
    public boolean isApplicable(Account account) {
        return account.getAccountType() == AccountType.SAVINGS;
    }

    @Override
    public String sourceRuleName() {
        return "MIN_BALANCE_FEE";
    }

    @Override
    public Optional<FeeDecision> evaluate(Account account, YearMonth period, FeeContext context) {
        if (context.getAverageDailyBalance().compareTo(feeProperties.getMinimumBalanceThreshold()) < 0) {
            return Optional.of(new FeeDecision(
                    FeeType.MINIMUM_BALANCE,
                    feeProperties.getMinimumBalanceFee(),
                    period.atEndOfMonth(),
                    "Average balance below minimum required for the month"
            ));
        }
        return Optional.empty();
    }
}
