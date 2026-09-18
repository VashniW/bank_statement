package com.bank.statements.repository;

import com.bank.statements.model.Fee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeeRepository extends JpaRepository<Fee, Long> {

    List<Fee> findAllByAccountIdAndPeriodYearAndPeriodMonth(
            Long accountId, Integer periodYear, Integer periodMonth);

    Optional<Fee> findByAccountIdAndPeriodYearAndPeriodMonthAndSourceRule(
            Long accountId, Integer periodYear, Integer periodMonth, String sourceRule);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM Fee f WHERE f.account.id = :accountId AND f.periodYear = :periodYear AND f.periodMonth = :periodMonth")
    void deleteAllByAccountIdAndPeriodYearAndPeriodMonth(
            @org.springframework.data.repository.query.Param("accountId") Long accountId,
            @org.springframework.data.repository.query.Param("periodYear") Integer periodYear,
            @org.springframework.data.repository.query.Param("periodMonth") Integer periodMonth);
}

