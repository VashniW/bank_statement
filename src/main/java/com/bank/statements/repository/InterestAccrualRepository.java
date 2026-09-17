package com.bank.statements.repository;

import com.bank.statements.model.InterestAccrual;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InterestAccrualRepository extends JpaRepository<InterestAccrual, Long> {

    Optional<InterestAccrual> findByAccountIdAndPeriodYearAndPeriodMonth(
            Long accountId, Integer periodYear, Integer periodMonth);
}
