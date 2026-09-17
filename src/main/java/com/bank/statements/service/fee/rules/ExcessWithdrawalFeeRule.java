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

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Optional;

/**
 * Charges a per-transaction fee for each ATM cash withdrawal beyond the free
 * monthly limit. Applies to SAVINGS accounts.
 *
 * Scoped to category "ATM" specifically, not every DEBIT transaction -
 * routine spending (shopping, bill payments, transfers) should never count
 * as a "withdrawal" here.
 */
@Component
@RequiredArgsConstructor
public class ExcessWithdrawalFeeRule implements FeeRule {

    private final FeeProperties feeProperties;

    @Override
    public boolean isApplicable(Account account) {
        return account.getAccountType() == AccountType.SAVINGS;
    }

    @Override
    public String sourceRuleName() {
        return "EXCESS_WITHDRAWAL_FEE";
    }

    @Override
    public Optional<FeeDecision> evaluate(Account account, YearMonth period, FeeContext context) {
        long freeLimit = feeProperties.getFreeWithdrawalsPerMonth();
        long excessCount = context.getAtmWithdrawalCount() - freeLimit;

        if (excessCount <= 0) {
            return Optional.empty();
        }

        BigDecimal amount = feeProperties.getExcessWithdrawalFee()
                .multiply(BigDecimal.valueOf(excessCount));

        return Optional.of(new FeeDecision(
                FeeType.EXCESS_WITHDRAWAL,
                amount,
                period.atEndOfMonth(),
                excessCount + " ATM withdrawal(s) beyond free monthly limit of " + freeLimit
        ));
    }
}
