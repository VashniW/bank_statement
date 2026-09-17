package com.bank.statements.controller;

import com.bank.statements.service.batch.BatchRunSummary;
import com.bank.statements.service.batch.MonthlyStatementBatchJob;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/batch")
@RequiredArgsConstructor
public class AdminBatchController {

    private final MonthlyStatementBatchJob batchJob;

    public record BatchRunRequest(Integer year, Integer month) {}

    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> getBatchStatus() {
        YearMonth current = YearMonth.now();
        YearMonth lastCompleted = current.minusMonths(1);
        return Map.of(
                "status", "ACTIVE",
                "currentPeriod", current.toString(),
                "lastCompletedPeriod", lastCompleted.toString(),
                "allowedPeriodMax", lastCompleted.toString()
        );
    }

    @PostMapping("/statements/run")
    @PreAuthorize("hasRole('ADMIN')")
    public BatchRunSummary runBatch(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestBody(required = false) BatchRunRequest body) {

        int resolvedYear;
        int resolvedMonth;

        if (body != null && body.year() != null && body.month() != null) {
            resolvedYear = body.year();
            resolvedMonth = body.month();
        } else if (year != null && month != null) {
            resolvedYear = year;
            resolvedMonth = month;
        } else {
            throw new IllegalArgumentException("Year and month must be provided via query parameters or JSON body.");
        }

        return batchJob.runForPeriod(YearMonth.of(resolvedYear, resolvedMonth));
    }
}
