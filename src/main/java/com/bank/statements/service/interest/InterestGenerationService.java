package com.bank.statements.service.interest;

import com.bank.statements.model.Account;
import com.bank.statements.model.InterestAccrual;
import com.bank.statements.model.InterestDirection;
import com.bank.statements.repository.InterestAccrualRepository;
import com.bank.statements.service.balance.DailyBalance;
import com.bank.statements.service.balance.PeriodBalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * Computes interest earned/charged for an account over a period using the
 * daily-balance method (standard bank practice): each day's balance is
 * multiplied by that day's rate, and the results are summed over the period.
 *
 * Idempotent: if an accrual already exists for (account, period), it's
 * returned as-is rather than recalculated - safe to call repeatedly.
 *
 * NOTE ON CREDIT ACCOUNTS: this uses the same balance sign convention as the
 * fee engine (CREDIT transactions add to balance, DEBIT transactions
 * subtract). For a real credit-card product, "amount owed" is usually
 * tracked as a positive number that purchases increase and payments
 * decrease - the opposite convention. This is a simplification for now;
 * flagging it as something to revisit once real credit-account semantics
 * are defined.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterestGenerationService {

    private static final int DAYS_IN_YEAR = 365;
    private static final int RATE_CALC_SCALE = 10;

    private final PeriodBalanceService periodBalanceService;
    private final InterestRateService interestRateService;
    private final InterestAccrualRepository interestAccrualRepository;

    public Optional<InterestAccrual> generateInterestForPeriod(Account account, YearMonth period) {
        Optional<InterestAccrual> existing = interestAccrualRepository
                .findByAccountIdAndPeriodYearAndPeriodMonth(
                        account.getId(), period.getYear(), period.getMonthValue());

        if (existing.isPresent()) {
            log.debug("Interest already accrued for account {} period {} - skipping",
                    account.getId(), period);
            return existing;
        }

        BigDecimal annualRate = interestRateService.getAnnualRate(account);
        if (annualRate == null || annualRate.compareTo(BigDecimal.ZERO) == 0) {
            return Optional.empty();
        }

        List<DailyBalance> dailyBalances = periodBalanceService.getDailyBalances(account, period);
        if (dailyBalances.isEmpty()) {
            return Optional.empty();
        }

        BigDecimal dailyRate = annualRate
                .divide(BigDecimal.valueOf(100), RATE_CALC_SCALE, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(DAYS_IN_YEAR), RATE_CALC_SCALE, RoundingMode.HALF_UP);

        InterestDirection direction = interestRateService.getDirection(account);

        // For EARNED interest (e.g. SAVINGS), customers only earn on positive daily balances.
        // Days with zero or negative balances earn 0.00 interest.
        BigDecimal totalInterest;
        if (direction == InterestDirection.EARNED) {
            totalInterest = dailyBalances.stream()
                    .map(DailyBalance::balance)
                    .filter(b -> b.compareTo(BigDecimal.ZERO) > 0)
                    .map(b -> b.multiply(dailyRate))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
        } else {
            totalInterest = dailyBalances.stream()
                    .map(db -> db.balance().abs().multiply(dailyRate))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        if (totalInterest.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }

        BigDecimal avgBalance = dailyBalances.stream()
                .map(DailyBalance::balance)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(dailyBalances.size()), 2, RoundingMode.HALF_UP);

        InterestAccrual accrual = InterestAccrual.builder()
                .account(account)
                .periodYear(period.getYear())
                .periodMonth(period.getMonthValue())
                .accrualDate(period.atEndOfMonth())
                .avgBalance(avgBalance)
                .rateApplied(annualRate)
                .amount(totalInterest.abs())
                .direction(direction)
                .description(direction == InterestDirection.EARNED
                        ? "Interest earned for the period"
                        : "Interest charged for the period")
                .createdAt(LocalDateTime.now())
                .build();

        InterestAccrual saved = interestAccrualRepository.save(accrual);
        log.info("Generated interest [{}] {} for account {} period {}",
                direction, totalInterest, account.getId(), period);
        return Optional.of(saved);
    }
}
