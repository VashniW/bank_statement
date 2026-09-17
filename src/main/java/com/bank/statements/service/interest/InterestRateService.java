package com.bank.statements.service.interest;

import com.bank.statements.config.InterestProperties;
import com.bank.statements.model.Account;
import com.bank.statements.model.InterestDirection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Looks up the applicable annual interest rate and direction (earned/charged)
 * for an account. Kept separate from the calculation logic so rate rules
 * (tiered by balance, promotional rates, product-specific rates) can evolve
 * independently later without touching InterestGenerationService.
 */
@Component
@RequiredArgsConstructor
public class InterestRateService {

    private final InterestProperties interestProperties;

    /** Annual rate as a percentage, e.g. 3.5 means 3.5% p.a. */
    public BigDecimal getAnnualRate(Account account) {
        return switch (account.getAccountType()) {
            case SAVINGS -> interestProperties.getSavingsAnnualRate();
            case CREDIT -> interestProperties.getCreditAnnualRate();
            case WALLET -> interestProperties.getWalletAnnualRate();
        };
    }

    public InterestDirection getDirection(Account account) {
        return switch (account.getAccountType()) {
            case SAVINGS, WALLET -> InterestDirection.EARNED;
            case CREDIT -> InterestDirection.CHARGED;
        };
    }
}
