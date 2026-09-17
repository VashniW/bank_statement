package com.bank.statements.service.fee;

import com.bank.statements.model.Account;

import java.time.YearMonth;
import java.util.Optional;

/**
 * One independent fee condition. Implementations are picked up automatically
 * as Spring beans and run by FeeGenerationService - adding a new fee type
 * means adding a new class here, no changes needed to the orchestration logic.
 */
public interface FeeRule {

    /** Whether this rule applies to this account at all (e.g. account type check). */
    boolean isApplicable(Account account);

    /** Evaluate the rule against the precomputed period context; empty if it doesn't fire. */
    Optional<FeeDecision> evaluate(Account account, YearMonth period, FeeContext context);

    /**
     * Stable identifier for this rule, stored on the Fee row as source_rule.
     * Used for idempotency - a rule won't fire twice for the same account/period.
     */
    String sourceRuleName();
}
