package com.bank.statements.service.fee.rules;

import com.bank.statements.config.FeeProperties;
import com.bank.statements.model.Account;
import com.bank.statements.model.AccountType;
import com.bank.statements.model.FeeType;
import com.bank.statements.model.Transaction;
import com.bank.statements.service.fee.FeeContext;
import com.bank.statements.service.fee.FeeDecision;
import com.bank.statements.service.fee.FeeRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.Optional;

/**
 * Flat monthly maintenance fee for SAVINGS accounts, waived if a salary
 * credit (category "SALARY") was received during the period - a common
 * waiver condition for salary accounts.
 */
@Component
@RequiredArgsConstructor
public class MonthlyMaintenanceFeeRule implements FeeRule {

    private static final String SALARY_CATEGORY = "SALARY";

    private final FeeProperties feeProperties;

    @Override
    public boolean isApplicable(Account account) {
        return account.getAccountType() == AccountType.SAVINGS;
    }

    @Override
    public String sourceRuleName() {
        return "MONTHLY_MAINTENANCE_FEE";
    }

    @Override
    public Optional<FeeDecision> evaluate(Account account, YearMonth period, FeeContext context) {
        boolean waived = context.getTransactions().stream()
                .map(Transaction::getCategory)
                .filter(c -> c != null)
                .anyMatch(SALARY_CATEGORY::equalsIgnoreCase);

        if (waived) {
            return Optional.empty();
        }

        return Optional.of(new FeeDecision(
                FeeType.MONTHLY_MAINTENANCE,
                feeProperties.getMonthlyMaintenanceFee(),
                period.atEndOfMonth(),
                "Monthly account maintenance fee"
        ));
    }
}
