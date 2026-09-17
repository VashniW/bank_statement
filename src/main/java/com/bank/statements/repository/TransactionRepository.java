package com.bank.statements.repository;

import com.bank.statements.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findAllByAccountIdAndTxnDateBetweenOrderByTxnDateAsc(
            Long accountId, LocalDate startDate, LocalDate endDate);

    List<Transaction> findAllByAccountIdAndTxnDateBefore(Long accountId, LocalDate date);

    /**
     * Earliest transaction date across the account's whole history - used as
     * the floor when recursively ensuring prior periods' statements exist
     * (no point recursing further back than the account's first activity).
     */
    @Query("SELECT MIN(t.txnDate) FROM Transaction t WHERE t.account.id = :accountId")
    Optional<LocalDate> findEarliestTxnDate(@Param("accountId") Long accountId);
}
