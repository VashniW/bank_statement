package com.bank.statements.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Annual interest rates (as percentages, e.g. 3.5 = 3.5% p.a.) by account type.
 * Bound from statement.interest.* in application.yml.
 */
@Component
@ConfigurationProperties(prefix = "statement.interest")
@Getter
@Setter
public class InterestProperties {

    private BigDecimal savingsAnnualRate;
    private BigDecimal creditAnnualRate;
    private BigDecimal walletAnnualRate;
}
