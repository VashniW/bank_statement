package com.bank.statements.dto;

import java.math.BigDecimal;

public record AccountDTO(
        Long id,
        String accountNumber,
        String accountType,
        String currency,
        String status,
        BigDecimal openingBalance,
        String ownerName) {

    public AccountDTO(Long id, String accountNumber, String accountType, String currency, String status, BigDecimal openingBalance) {
        this(id, accountNumber, accountType, currency, status, openingBalance, null);
    }
}