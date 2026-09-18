package com.bank.statements.service.statement;

import com.bank.statements.model.Account;
import com.bank.statements.model.AccountStatus;
import com.bank.statements.model.AccountType;
import com.bank.statements.model.Statement;
import com.bank.statements.repository.AccountRepository;
import com.bank.statements.repository.StatementRepository;
import com.bank.statements.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class StatementRecursionTest {

    @Autowired
    private StatementService statementService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private StatementRepository statementRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testRecursionFromAccountOpenedDate() {
        Account vikram = accountRepository.findByAccountNumber("SAV-0002-0004").orElseThrow();

        // Requesting May 2026 must recursively ensure Jan, Feb, Mar, Apr exist down to openedDate (Jan 2026)
        StatementResult may = statementService.getStatement(vikram, YearMonth.of(2026, 5));
        assertNotNull(may);

        List<Statement> allStatements = statementRepository.findAllByAccountIdOrderByPeriodAsc(vikram.getId());
        assertTrue(allStatements.size() >= 5, "Should have statements for at least Jan through May");

        // Verify January starts with the account's configured opening balance
        assertEquals(0, allStatements.get(0).getOpeningBalance().compareTo(new BigDecimal("25000.00")));

        // Verify strict continuity: each month's opening balance must equal the prior month's closing balance
        for (int i = 1; i < allStatements.size(); i++) {
            Statement prev = allStatements.get(i - 1);
            Statement curr = allStatements.get(i);
            assertEquals(0, curr.getOpeningBalance().compareTo(prev.getClosingBalance()),
                    "Period " + curr.getPeriodYear() + "-" + curr.getPeriodMonth() +
                            " opening balance must match " + prev.getPeriodYear() + "-" + prev.getPeriodMonth() + " closing balance");
        }
    }

    @Test
    void testAccountOpenedLaterInYearStopsAtOpenedDate() {
        var user = userRepository.findAll().get(0);
        Account marchAccount = accountRepository.save(Account.builder()
                .user(user)
                .accountNumber("TEST-MARCH-0001")
                .accountType(AccountType.SAVINGS)
                .currency("INR")
                .status(AccountStatus.ACTIVE)
                .openedDate(LocalDate.of(2026, 3, 1))
                .openingBalance(BigDecimal.valueOf(15000))
                .build());

        // May statement should recurse down to March (the opened month) and stop there
        StatementResult may = statementService.getStatement(marchAccount, YearMonth.of(2026, 5));
        assertNotNull(may);

        List<Statement> statements = statementRepository.findAllByAccountIdOrderByPeriodAsc(marchAccount.getId());
        assertEquals(3, statements.size(), "Should have statements for March, April, May only");
        assertEquals(3, statements.get(0).getPeriodMonth(), "Earliest statement must be March");
        assertEquals(0, statements.get(0).getOpeningBalance().compareTo(new BigDecimal("15000.00")),
                "March opening balance must be the account's 15k opening balance");

        // Attempting to request February (prior to opening date) must be rejected
        assertThrows(IllegalArgumentException.class, () ->
                statementService.getStatement(marchAccount, YearMonth.of(2026, 2)));
    }
}
