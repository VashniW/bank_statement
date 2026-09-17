package com.bank.statements.repository;

import com.bank.statements.model.Statement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface StatementRepository extends JpaRepository<Statement, Long> {

    Optional<Statement> findByAccountIdAndPeriodYearAndPeriodMonth(
            Long accountId, Integer periodYear, Integer periodMonth);

    List<Statement> findAllByAccountIdOrderByPeriodYearDescPeriodMonthDesc(Long accountId);

    /**
     * Finds the most recent statement strictly before the given period (correctly
     * handles year boundaries, e.g. Dec 2025 is "previous" to Jan 2026) - used to
     * carry forward the opening balance for the next period's statement.
     */
    @Query("""
            SELECT s FROM Statement s
            WHERE s.account.id = :accountId
              AND (s.periodYear < :periodYear
                   OR (s.periodYear = :periodYear AND s.periodMonth < :periodMonth))
            ORDER BY s.periodYear DESC, s.periodMonth DESC
            """)
    List<Statement> findPreviousStatements(
            @Param("accountId") Long accountId,
            @Param("periodYear") Integer periodYear,
            @Param("periodMonth") Integer periodMonth);

    /**
     * Convenience wrapper - returns just the single most recent previous statement,
     * which is what's needed for opening-balance carry-forward.
     */
    default Optional<Statement> findMostRecentPrevious(Long accountId, Integer periodYear, Integer periodMonth) {
        List<Statement> results = findPreviousStatements(accountId, periodYear, periodMonth);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Finds the earliest persisted statement for an account (oldest period year/month).
     * Used as a secondary floor by the chain-generation logic: if a statement
     * already exists for a period that predates the earliest transaction, the
     * chain must still be filled-in from that statement forward (not just from
     * the earliest transaction forward).
     */
    @Query("""
            SELECT s FROM Statement s
            WHERE s.account.id = :accountId
            ORDER BY s.periodYear ASC, s.periodMonth ASC
            """)
    List<Statement> findAllByAccountIdOrderByPeriodAsc(@Param("accountId") Long accountId);

    default Optional<YearMonth> findEarliestStatementPeriod(Long accountId) {
        List<Statement> results = findAllByAccountIdOrderByPeriodAsc(accountId);
        if (results.isEmpty()) return Optional.empty();
        Statement earliest = results.get(0);
        return Optional.of(YearMonth.of(earliest.getPeriodYear(), earliest.getPeriodMonth()));
    }
}
