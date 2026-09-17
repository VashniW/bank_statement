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
}
