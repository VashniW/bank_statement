package com.bank.statements.repository;

import com.bank.statements.model.Account;
import com.bank.statements.model.AccountStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findAllByStatus(AccountStatus status);

    @EntityGraph(attributePaths = {"user"})
    List<Account> findAllByUserId(Long userId);

    @EntityGraph(attributePaths = {"user"})
    @Override
    List<Account> findAll();

    Optional<Account> findByAccountNumber(String accountNumber);

    @EntityGraph(attributePaths = {"user"})
    @Override
    Optional<Account> findById(Long id);

    /**
     * Acquires a DB-level row lock on this account for the duration of the
     * current transaction. Used before fee/interest/statement generation so
     * that concurrent requests for the same account (e.g. a batch run and an
     * on-demand statement view happening at the same time) serialize here
     * instead of racing against each other's unique-constraint checks.
     */
    @EntityGraph(attributePaths = {"user"})
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> lockForUpdate(@Param("id") Long id);
}
