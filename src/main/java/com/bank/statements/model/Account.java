package com.bank.statements.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    @Builder.Default
    @Column(name = "opened_date", nullable = false, columnDefinition = "date default '2026-01-01' not null")
    private LocalDate openedDate = LocalDate.of(2026, 1, 1);


    /**
     * The balance this account carried upon opening / onboarding.
     */
    @Builder.Default
    @Column(name = "opening_balance", nullable = false, precision = 19, scale = 2, columnDefinition = "numeric(19,2) default 0 not null")
    private BigDecimal openingBalance = BigDecimal.ZERO;
}