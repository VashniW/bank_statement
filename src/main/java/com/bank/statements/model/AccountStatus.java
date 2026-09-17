package com.bank.statements.model;

/**
 * Represents the operational state of an account.
 * ACTIVE     - normal, usable account; included in monthly batch generation.
 * INACTIVE   - dormant / no recent activity, but not closed.
 * FROZEN     - temporarily blocked (fraud hold, compliance review, etc.).
 * CLOSED     - account closed; no new transactions, but historical statements
 *              remain downloadable.
 */
public enum AccountStatus {
    ACTIVE,
    INACTIVE,
    FROZEN,
    CLOSED
}
