package com.bank.statements.controller;

import com.bank.statements.dto.StatementListItemDTO;
import com.bank.statements.dto.StatementSummaryDTO;
import com.bank.statements.exception.AccountNotFoundException;
import com.bank.statements.mapper.StatementMapper;
import com.bank.statements.model.Account;
import com.bank.statements.repository.AccountRepository;
import com.bank.statements.repository.StatementRepository;
import com.bank.statements.security.UserPrincipal;
import com.bank.statements.service.pdf.PdfGeneratorService;
import com.bank.statements.service.statement.StatementResult;
import com.bank.statements.service.statement.StatementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.YearMonth;
import java.util.List;

/**
 * Access is enforced against the real authenticated user, resolved from
 * the JWT via Spring Security's Authentication object (populated by
 * JwtAuthenticationFilter).
 *
 * Users can only access their own accounts. Administrators (ROLE_ADMIN)
 * have authorized access to view and verify statements for any account.
 */
@RestController
@RequestMapping("/api/accounts/{accountId}/statements")
@RequiredArgsConstructor
public class AccountStatementController {

    private final AccountRepository accountRepository;
    private final StatementRepository statementRepository;
    private final StatementService statementService;
    private final PdfGeneratorService pdfGeneratorService;

    @GetMapping
    public List<StatementListItemDTO> listStatements(
            @PathVariable Long accountId,
            Authentication authentication) {

        getAccessibleAccount(accountId, authentication);

        return statementRepository.findAllByAccountIdOrderByPeriodYearDescPeriodMonthDesc(accountId)
                .stream()
                .map(StatementMapper::toListItemDTO)
                .toList();
    }

    @GetMapping("/{year}/{month}")
    public StatementSummaryDTO getStatement(
            @PathVariable Long accountId,
            @PathVariable int year,
            @PathVariable int month,
            Authentication authentication) {

        Account account = getAccessibleAccount(accountId, authentication);
        YearMonth period = YearMonth.of(year, month);

        StatementResult result = statementService.getStatement(account, period);
        return StatementMapper.toSummaryDTO(account, result);
    }

    @GetMapping("/{year}/{month}/pdf")
    public ResponseEntity<byte[]> downloadStatementPdf(
            @PathVariable Long accountId,
            @PathVariable int year,
            @PathVariable int month,
            Authentication authentication) throws IOException {

        Account account = getAccessibleAccount(accountId, authentication);
        YearMonth period = YearMonth.of(year, month);

        StatementResult result = statementService.getStatement(account, period);
        byte[] pdfBytes = pdfGeneratorService.getOrGeneratePdf(account, result);

        String filename = String.format("statement-%s-%d-%02d.pdf",
                account.getAccountNumber(), year, month);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    private Account getAccessibleAccount(Long accountId, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        return accountRepository.findById(accountId)
                .filter(account -> isAdmin || account.getUser().getId().equals(principal.getId()))
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }
}
