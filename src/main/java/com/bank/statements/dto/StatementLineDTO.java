package com.bank.statements.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StatementLineDTO(
        LocalDate date,
        String description,
        String sourceType,
        String dcIndicator,
        BigDecimal amount,
        BigDecimal balanceAfter) {
}
