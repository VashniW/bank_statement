package com.bank.statements.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StatementListItemDTO(
        Long statementId,
        int periodYear,
        int periodMonth,
        String status,
        LocalDate periodEndDate,
        BigDecimal closingBalance) {
}
