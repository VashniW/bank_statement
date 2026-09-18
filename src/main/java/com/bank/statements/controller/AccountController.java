package com.bank.statements.controller;

import com.bank.statements.dto.AccountDTO;
import com.bank.statements.model.Account;
import com.bank.statements.repository.AccountRepository;
import com.bank.statements.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountRepository accountRepository;

    @GetMapping
    public List<AccountDTO> listMyAccounts(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        List<Account> accounts = isAdmin
                ? accountRepository.findAll()
                : accountRepository.findAllByUserId(principal.getId());

        return accounts.stream()
                .map(a -> new AccountDTO(
                        a.getId(),
                        a.getAccountNumber(),
                        a.getAccountType().name(),
                        a.getCurrency(),
                        a.getStatus().name(),
                        a.getOpeningBalance(),
                        a.getOpenedDate(),
                        a.getUser() != null ? a.getUser().getName() : null))
                .toList();

    }
}