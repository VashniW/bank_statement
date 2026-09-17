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
        name = "statement",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_statement_account_period",
                columnNames = {"account_id", "period_year", "period_month"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Statement {

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

    /**
     * Last date actually covered by this statement.
     * Full month-end for GENERATED statements; today's date for PROVISIONAL ones.
     */
    @Column(name = "period_end_date", nullable = false)
    private LocalDate periodEndDate;

    @Column(name = "opening_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal openingBalance;

    @Column(name = "closing_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal closingBalance;

    @Column(name = "total_debits", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalDebits;

    @Column(name = "total_credits", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalCredits;

    @Column(name = "total_fees", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalFees;

    @Column(name = "total_interest", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalInterest;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatementStatus status;

    @Column(name = "pdf_path", length = 500)
    private String pdfPath;
}
