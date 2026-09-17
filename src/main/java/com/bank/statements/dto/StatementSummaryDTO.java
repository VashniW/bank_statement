package com.bank.statements.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record StatementSummaryDTO(
        Long accountId,
        String accountNumber,
        String accountHolder,
        int periodYear,
        int periodMonth,
        String status,
        LocalDate periodEndDate,
        BigDecimal openingBalance,
        BigDecimal closingBalance,
        BigDecimal totalDebits,
        BigDecimal totalCredits,
        BigDecimal totalFees,
        BigDecimal totalInterest,
        List<StatementLineDTO> lines) {
}
