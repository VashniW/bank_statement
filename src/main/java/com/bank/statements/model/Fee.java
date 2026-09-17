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
        name = "fee",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_fee_account_period_rule",
                columnNames = {"account_id", "period_year", "period_month", "source_rule"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fee {

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

    @Column(name = "fee_date", nullable = false)
    private LocalDate feeDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_type", nullable = false, length = 30)
    private FeeType feeType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String description;

    /** Identifies which FeeRule produced this row - used for idempotency checks. */
    @Column(name = "source_rule", nullable = false, length = 100)
    private String sourceRule;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
