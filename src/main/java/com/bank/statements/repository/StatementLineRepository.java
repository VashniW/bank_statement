package com.bank.statements.repository;

import com.bank.statements.model.StatementLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StatementLineRepository extends JpaRepository<StatementLine, Long> {

    List<StatementLine> findAllByStatementIdOrderByLineDateAsc(Long statementId);
}
