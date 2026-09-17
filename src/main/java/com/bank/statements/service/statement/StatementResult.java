package com.bank.statements.service.statement;

import com.bank.statements.model.Statement;
import com.bank.statements.model.StatementLine;

import java.util.List;

/**
 * Pairs a Statement with its line items. For GENERATED statements both are
 * persisted entities (have IDs). For PROVISIONAL statements neither is
 * persisted - they're computed fresh on every call and exist only for this
 * response.
 */
public record StatementResult(Statement statement, List<StatementLine> lines) {
}
