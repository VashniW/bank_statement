package com.bank.statements.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "statement.fees")
@Getter
@Setter
public class FeeProperties {

    private BigDecimal minimumBalanceThreshold;
    private BigDecimal minimumBalanceFee;
    private BigDecimal overdraftFee;
    private BigDecimal monthlyMaintenanceFee;
    private int freeWithdrawalsPerMonth;
    private BigDecimal excessWithdrawalFee;
}
