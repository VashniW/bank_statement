package com.bank.statements.service.batch;

import java.time.YearMonth;
import java.util.List;

public record BatchRunSummary(
        YearMonth period,
        int totalAccounts,
        int succeeded,
        int failed,
        List<String> failureDetails) {
}
