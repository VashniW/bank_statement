package com.bank.statements.service.pdf;

import com.bank.statements.model.Account;
import com.bank.statements.service.statement.StatementResult;
import com.bank.statements.service.statement.StatementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PdfGeneratorServiceTest {

    @Autowired
    private PdfGeneratorService pdfGeneratorService;

    @Autowired
    private StatementService statementService;

    @Autowired
    private com.bank.statements.repository.AccountRepository accountRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.bank.statements.security.JwtService jwtService;

    @Autowired
    private com.bank.statements.security.CustomUserDetailsService userDetailsService;

    @Test
    void testPdfGeneration() throws Exception {
        Account account = accountRepository.findAll().get(0);
        StatementResult result = statementService.getStatement(account, YearMonth.of(2026, 7));
        byte[] pdf = pdfGeneratorService.getOrGeneratePdf(account, result);
        assertNotNull(pdf);
    }

    @Test
    void testDownloadPdfEndpoint() throws Exception {
        var userDetails = (com.bank.statements.security.UserPrincipal) userDetailsService.loadUserByUsername("asha.rao@example.com");
        String token = jwtService.generateToken(userDetails);

        mockMvc.perform(get("/api/accounts/1/statements/2026/7/pdf")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().exists("Content-Disposition"));
    }
}
