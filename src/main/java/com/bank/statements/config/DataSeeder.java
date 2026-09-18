package com.bank.statements.config;

import com.bank.statements.model.*;
import com.bank.statements.repository.AccountRepository;
import com.bank.statements.repository.TransactionRepository;
import com.bank.statements.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Seeds sample users/accounts/transactions for local/dev testing.
 *
 * IMPORTANT: this only runs when the database is empty (checked via
 * userRepository.count() == 0). The H2 database is file-based, so data
 * persists across app restarts - this seeder will NOT re-run or duplicate
 * data on subsequent startups, it only seeds once, ever, on a fresh DB.
 *
 * All seeded users share the password "password123" (BCrypt-hashed below)
 * so you can log in via POST /api/auth/login while testing locally.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private static final String SEED_PASSWORD = "password123";

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already seeded - skipping seed data load.");
            accountRepository.findAll().forEach(acc -> {
                boolean changed = false;
                if (acc.getOpenedDate() == null) {
                    acc.setOpenedDate(LocalDate.of(2026, 1, 1));
                    changed = true;
                }
                if ("SAV-0002-0004".equals(acc.getAccountNumber()) &&
                        (acc.getOpeningBalance() == null || acc.getOpeningBalance().compareTo(BigDecimal.valueOf(25000)) != 0)) {
                    acc.setOpeningBalance(BigDecimal.valueOf(25000));
                    changed = true;
                }
                if (changed) {
                    accountRepository.save(acc);
                }
            });
            return;
        }

        log.info("Empty database detected - loading seed data.");
        seed();
    }

    private void seed() {
        String hashedPassword = passwordEncoder.encode(SEED_PASSWORD);

        User asha = userRepository.save(User.builder()
                .name("Asha Rao")
                .email("asha.rao@example.com")
                .passwordHash(hashedPassword)
                .roles("USER")
                .build());

        User vikram = userRepository.save(User.builder()
                .name("Vikram Shah")
                .email("vikram.shah@example.com")
                .passwordHash(hashedPassword)
                .roles("USER")
                .build());

        userRepository.save(User.builder()
                .name("Admin User")
                .email("admin@example.com")
                .passwordHash(hashedPassword)
                .roles("ADMIN")
                .build());

        Account savings1 = accountRepository.save(Account.builder()
                .user(asha)
                .accountNumber("SAV-0001-0001")
                .accountType(AccountType.SAVINGS)
                .currency("INR")
                .status(AccountStatus.ACTIVE)
                .openedDate(LocalDate.of(2026, 1, 1))
                .build());

        Account credit1 = accountRepository.save(Account.builder()
                .user(asha)
                .accountNumber("CRD-0001-0002")
                .accountType(AccountType.CREDIT)
                .currency("INR")
                .status(AccountStatus.ACTIVE)
                .openedDate(LocalDate.of(2026, 1, 1))
                .build());

        Account wallet1 = accountRepository.save(Account.builder()
                .user(vikram)
                .accountNumber("WAL-0002-0003")
                .accountType(AccountType.WALLET)
                .currency("INR")
                .status(AccountStatus.ACTIVE)
                .openedDate(LocalDate.of(2026, 1, 1))
                .build());

        // Zero-activity account - tests the "no transactions in period" statement case.
        accountRepository.save(Account.builder()
                .user(vikram)
                .accountNumber("SAV-0002-0004")
                .accountType(AccountType.SAVINGS)
                .currency("INR")
                .status(AccountStatus.ACTIVE)
                .openedDate(LocalDate.of(2026, 1, 1))
                .openingBalance(BigDecimal.valueOf(25000))
                .build());


        // --- Savings account 1: July 2026 ---
        txn(savings1, "2026-07-02", 50000, TransactionType.CREDIT, "SALARY", "Salary credit - July");
        txn(savings1, "2026-07-05", 3500, TransactionType.DEBIT, "SHOPPING", "Online purchase");
        txn(savings1, "2026-07-10", 1200, TransactionType.DEBIT, "UTILITIES", "Electricity bill");
        txn(savings1, "2026-07-15", 2000, TransactionType.DEBIT, "ATM", "ATM withdrawal");
        txn(savings1, "2026-07-20", 2000, TransactionType.DEBIT, "ATM", "ATM withdrawal");
        txn(savings1, "2026-07-22", 2000, TransactionType.DEBIT, "ATM", "ATM withdrawal");
        txn(savings1, "2026-07-25", 5000, TransactionType.CREDIT, "TRANSFER", "Transfer from friend");

        // --- Savings account 1: August 2026 ---
        txn(savings1, "2026-08-01", 50000, TransactionType.CREDIT, "SALARY", "Salary credit - August");
        txn(savings1, "2026-08-06", 4200, TransactionType.DEBIT, "SHOPPING", "Grocery shopping");
        txn(savings1, "2026-08-12", 1500, TransactionType.DEBIT, "UTILITIES", "Internet bill");
        txn(savings1, "2026-08-18", 60000, TransactionType.DEBIT, "TRANSFER", "Large transfer out");
        txn(savings1, "2026-08-28", 10000, TransactionType.CREDIT, "TRANSFER", "Refund received");

        // --- Credit account: July & August 2026 ---
        txn(credit1, "2026-07-08", 15000, TransactionType.DEBIT, "SHOPPING", "Electronics purchase");
        txn(credit1, "2026-07-18", 5000, TransactionType.CREDIT, "PAYMENT", "Bill payment received");
        txn(credit1, "2026-08-03", 8000, TransactionType.DEBIT, "DINING", "Restaurant charges");
        txn(credit1, "2026-08-20", 3000, TransactionType.CREDIT, "PAYMENT", "Partial bill payment");

        // --- Wallet account: August 2026 only ---
        txn(wallet1, "2026-08-02", 2000, TransactionType.CREDIT, "TOPUP", "Wallet top-up");
        txn(wallet1, "2026-08-14", 450, TransactionType.DEBIT, "FOOD", "Food delivery order");
        txn(wallet1, "2026-08-26", 300, TransactionType.DEBIT, "TRANSPORT", "Cab ride");

        log.info("Seed data loaded: 3 users, 4 accounts, transactions across Jul-Aug 2026.");
        log.info("All seeded users share password '{}' for local login testing.", SEED_PASSWORD);
    }

    private void txn(Account account, String date, double amount, TransactionType type,
                      String category, String description) {
        LocalDate txnDate = LocalDate.parse(date);
        transactionRepository.save(Transaction.builder()
                .account(account)
                .txnDate(txnDate)
                .valueDate(txnDate)
                .amount(BigDecimal.valueOf(amount))
                .type(type)
                .category(category)
                .description(description)
                .createdAt(LocalDateTime.now())
                .build());
    }
}