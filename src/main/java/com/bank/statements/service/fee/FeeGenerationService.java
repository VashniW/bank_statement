package com.bank.statements.service.fee;

import com.bank.statements.model.Account;
import com.bank.statements.model.Fee;
import com.bank.statements.repository.FeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs every applicable FeeRule against an account for a given period and
 * persists any resulting Fee rows.
 *
 * Idempotent: if a fee already exists for (account, period, rule), that rule
 * is skipped - safe to call multiple times (e.g. batch re-run, on-demand
 * statement generation) without duplicate charges.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeeGenerationService {

    private final List<FeeRule> feeRules;
    private final FeeRepository feeRepository;
    private final FeeContextBuilder feeContextBuilder;

    public List<Fee> generateFeesForPeriod(Account account, YearMonth period) {
        FeeContext context = feeContextBuilder.build(account, period);
        List<Fee> generatedFees = new ArrayList<>();

        for (FeeRule rule : feeRules) {
            if (!rule.isApplicable(account)) {
                continue;
            }

            String sourceRule = rule.sourceRuleName();
            boolean alreadyExists = feeRepository
                    .findByAccountIdAndPeriodYearAndPeriodMonthAndSourceRule(
                            account.getId(), period.getYear(), period.getMonthValue(), sourceRule)
                    .isPresent();

            if (alreadyExists) {
                log.debug("Fee for rule {} already exists for account {} period {} - skipping",
                        sourceRule, account.getId(), period);
                continue;
            }

            rule.evaluate(account, period, context).ifPresent(decision -> {
                Fee fee = Fee.builder()
                        .account(account)
                        .periodYear(period.getYear())
                        .periodMonth(period.getMonthValue())
                        .feeDate(decision.feeDate())
                        .feeType(decision.feeType())
                        .amount(decision.amount())
                        .description(decision.description())
                        .sourceRule(sourceRule)
                        .createdAt(LocalDateTime.now())
                        .build();

                generatedFees.add(feeRepository.save(fee));
                log.info("Generated fee [{}] {} for account {} period {}",
                        sourceRule, decision.amount(), account.getId(), period);
            });
        }

        return generatedFees;
    }
}
