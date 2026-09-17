package com.bank.statements.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "interest_accrual",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_interest_account_period",
                columnNames = {"account_id", "period_year", "period_month"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterestAccrual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "period_year", nullable = false)
    private Integer periodYear;

    @Column(name = "period_month", nullable = false)
    private Integer periodMonth;

    @Column(name = "accrual_date", nullable = false)
    private LocalDate accrualDate;

    /** Average daily balance used for the calculation, kept for audit/traceability. */
    @Column(name = "avg_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal avgBalance;

    @Column(name = "rate_applied", nullable = false, precision = 6, scale = 3)
    private BigDecimal rateApplied;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private InterestDirection direction;

    @Column(length = 255)
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
