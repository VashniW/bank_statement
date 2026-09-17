package com.bank.statements.service.balance;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * End-of-day balance for a single date, produced by DailyBalanceCalculator.
 */
public record DailyBalance(LocalDate date, BigDecimal balance) {
}
