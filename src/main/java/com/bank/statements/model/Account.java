package com.bank.statements.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "account")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "account_number", nullable = false, unique = true, length = 34)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    /**
     * The balance this account actually carried before this system's
     * transaction history begins - NOT the same as "opening date" (which we
     * deliberately don't track). Needed because an onboarded/migrated
     * account may have had a real non-zero balance that predates every
     * Transaction row we hold for it. Defaults to 0 for accounts whose full
     * history genuinely starts here (e.g. all seeded demo accounts).
     *
     * This is the true floor PeriodBalanceService falls back to - NOT a
     * hardcoded zero - once it runs out of prior Statement records or
     * transaction history to chain back through.
     */
    @Builder.Default
    @Column(name = "opening_balance", nullable = false, precision = 19, scale = 2, columnDefinition = "numeric(19,2) default 0 not null")
    private BigDecimal openingBalance = BigDecimal.ZERO;
}