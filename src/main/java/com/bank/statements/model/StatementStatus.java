package com.bank.statements.model;

/**
 * GENERATED  - final statement for a completed month; persisted permanently,
 *              immutable, covers the full calendar month.
 * PROVISIONAL - statement for the current, still in-progress month; computed
 *               live on each request, covers 1st of month through today only.
 *               Not treated as final since fees/interest for the month are
 *               not yet fully accrued.
 */
public enum StatementStatus {
    GENERATED,
    PROVISIONAL
}
