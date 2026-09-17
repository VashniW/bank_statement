package com.bank.statements.mapper;

import com.bank.statements.dto.StatementLineDTO;
import com.bank.statements.dto.StatementListItemDTO;
import com.bank.statements.dto.StatementSummaryDTO;
import com.bank.statements.model.Account;
import com.bank.statements.model.Statement;
import com.bank.statements.model.StatementLine;
import com.bank.statements.service.statement.StatementResult;

public final class StatementMapper {

    private StatementMapper() {
    }

    public static StatementListItemDTO toListItemDTO(Statement statement) {
        return new StatementListItemDTO(
                statement.getId(),
                statement.getPeriodYear(),
                statement.getPeriodMonth(),
                statement.getStatus().name(),
                statement.getPeriodEndDate(),
                statement.getClosingBalance());
    }

    public static StatementSummaryDTO toSummaryDTO(Account account, StatementResult result) {
        Statement statement = result.statement();
        String accountHolder = account.getUser() != null ? account.getUser().getName() : null;

        return new StatementSummaryDTO(
                account.getId(),
                account.getAccountNumber(),
                accountHolder,
                statement.getPeriodYear(),
                statement.getPeriodMonth(),
                statement.getStatus().name(),
                statement.getPeriodEndDate(),
                statement.getOpeningBalance(),
                statement.getClosingBalance(),
                statement.getTotalDebits(),
                statement.getTotalCredits(),
                statement.getTotalFees(),
                statement.getTotalInterest(),
                result.lines().stream().map(StatementMapper::toLineDTO).toList());
    }

    public static StatementLineDTO toLineDTO(StatementLine line) {
        return new StatementLineDTO(
                line.getLineDate(),
                line.getDescription(),
                line.getSourceType().name(),
                line.getDcIndicator().name(),
                line.getAmount(),
                line.getBalanceAfter());
    }
}
