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
 * Charges a flat fee if the account balance went negative on any day during
 * the period. Applies to SAVINGS and CREDIT accounts (wallets typically
 * cannot go negative).
 */
@Component
@RequiredArgsConstructor
public class OverdraftFeeRule implements FeeRule {

    private final FeeProperties feeProperties;

    @Override
    public boolean isApplicable(Account account) {
        return account.getAccountType() == AccountType.SAVINGS
                || account.getAccountType() == AccountType.CREDIT;
    }

    @Override
    public String sourceRuleName() {
        return "OVERDRAFT_FEE";
    }

    @Override
    public Optional<FeeDecision> evaluate(Account account, YearMonth period, FeeContext context) {
        if (context.isHadNegativeBalanceDay()) {
            return Optional.of(new FeeDecision(
                    FeeType.OVERDRAFT,
                    feeProperties.getOverdraftFee(),
                    period.atEndOfMonth(),
                    "Account balance went negative during the period"
            ));
        }
        return Optional.empty();
    }
}
