package com.bank.statements.service.fee;

import com.bank.statements.model.FeeType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FeeDecision(FeeType feeType, BigDecimal amount, LocalDate feeDate, String description) {
}
