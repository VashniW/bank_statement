package com.bank.statements.repository;

import com.bank.statements.model.StatementLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StatementLineRepository extends JpaRepository<StatementLine, Long> {

    List<StatementLine> findAllByStatementIdOrderByLineDateAsc(Long statementId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM StatementLine sl WHERE sl.statement.id = :statementId")
    void deleteAllByStatementId(@org.springframework.data.repository.query.Param("statementId") Long statementId);
}

