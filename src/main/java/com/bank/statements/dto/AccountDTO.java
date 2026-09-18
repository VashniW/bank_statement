package com.bank.statements.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AccountDTO(
        Long id,
        String accountNumber,
        String accountType,
        String currency,
        String status,
        BigDecimal openingBalance,
        LocalDate openedDate,
        String ownerName) {

    public AccountDTO(Long id, String accountNumber, String accountType, String currency, String status, BigDecimal openingBalance, String ownerName) {
        this(id, accountNumber, accountType, currency, status, openingBalance, null, ownerName);
    }

    public AccountDTO(Long id, String accountNumber, String accountType, String currency, String status, BigDecimal openingBalance) {
        this(id, accountNumber, accountType, currency, status, openingBalance, null, null);
    }
}