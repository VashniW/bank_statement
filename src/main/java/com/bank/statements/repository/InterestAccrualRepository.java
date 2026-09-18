package com.bank.statements.repository;

import com.bank.statements.model.InterestAccrual;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InterestAccrualRepository extends JpaRepository<InterestAccrual, Long> {

    Optional<InterestAccrual> findByAccountIdAndPeriodYearAndPeriodMonth(
            Long accountId, Integer periodYear, Integer periodMonth);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM InterestAccrual ia WHERE ia.account.id = :accountId AND ia.periodYear = :periodYear AND ia.periodMonth = :periodMonth")
    void deleteByAccountIdAndPeriodYearAndPeriodMonth(
            @org.springframework.data.repository.query.Param("accountId") Long accountId,
            @org.springframework.data.repository.query.Param("periodYear") Integer periodYear,
            @org.springframework.data.repository.query.Param("periodMonth") Integer periodMonth);
}

