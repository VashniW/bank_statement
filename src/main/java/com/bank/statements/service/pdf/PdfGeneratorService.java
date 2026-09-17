package com.bank.statements.service.pdf;

import com.bank.statements.config.PdfProperties;
import com.bank.statements.dto.StatementSummaryDTO;
import com.bank.statements.mapper.StatementMapper;
import com.bank.statements.model.Account;
import com.bank.statements.model.Statement;
import com.bank.statements.repository.StatementRepository;
import com.bank.statements.service.statement.StatementResult;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Renders a statement to a password-protected PDF.
 *
 * GENERATED (final, past-month) statements are cached to disk after the
 * first render, keyed by account + period - subsequent requests just read
 * the file back instead of re-rendering. PROVISIONAL (current month)
 * statements are never cached, consistent with how the underlying
 * statement data itself is always recomputed fresh for the current month.
 *
 * @Transactional here matters for the same reason it did for
 * StatementService: account.getUser().getName() is a lazy-loaded
 * association, and without an active transaction accessing it here could
 * fail the same way the pessimistic lock query did earlier.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PdfGeneratorService {

    private final TemplateEngine templateEngine;
    private final StatementPdfPasswordService passwordService;
    private final StatementRepository statementRepository;
    private final PdfProperties pdfProperties;

    @Transactional
    public byte[] getOrGeneratePdf(Account account, StatementResult result) throws IOException {
        Statement statement = result.statement();

        if (statement.getId() != null && statement.getPdfPath() != null) {
            Path cached = Path.of(statement.getPdfPath());
            if (Files.exists(cached)) {
                log.debug("Serving cached PDF for statement {}", statement.getId());
                return Files.readAllBytes(cached);
            }
        }

        byte[] pdfBytes = renderAndEncrypt(account, result);

        if (statement.getId() != null) {
            Path path = storePdf(account, statement, pdfBytes);
            statement.setPdfPath(path.toString());
            statementRepository.save(statement);
        }

        return pdfBytes;
    }

    private byte[] renderAndEncrypt(Account account, StatementResult result) throws IOException {
        String html = renderHtml(account, result);
        byte[] rawPdf = htmlToPdf(html);
        String userPassword = passwordService.generatePassword(account);
        return encryptPdf(rawPdf, userPassword);
    }

    private String renderHtml(Account account, StatementResult result) {
        StatementSummaryDTO dto = StatementMapper.toSummaryDTO(account, result);

        Context context = new Context();
        context.setVariable("holderName", account.getUser().getName());
        context.setVariable("statement", dto);

        return templateEngine.process("statement", context);
    }

    private byte[] htmlToPdf(String html) throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        }
    }

    private byte[] encryptPdf(byte[] rawPdf, String userPassword) throws IOException {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(rawPdf));
             ByteArrayOutputStream os = new ByteArrayOutputStream()) {

            AccessPermission permission = new AccessPermission();
            permission.setCanPrint(true);
            permission.setCanModify(false);
            permission.setCanExtractContent(false);
            permission.setCanModifyAnnotations(false);

            // Owner password unlocks full permissions regardless of the user
            // password - kept separate so knowing the (derivable) user
            // password pattern doesn't also grant edit/print override rights.
            StandardProtectionPolicy policy = new StandardProtectionPolicy(
                    pdfProperties.getOwnerPassword(), userPassword, permission);
            policy.setEncryptionKeyLength(128);

            document.protect(policy);
            document.save(os);
            return os.toByteArray();
        }
    }

    private Path storePdf(Account account, Statement statement, byte[] pdfBytes) throws IOException {
        Path dir = Path.of(pdfProperties.getStoragePath(), String.valueOf(account.getId()));
        Files.createDirectories(dir);

        String filename = String.format("%d-%02d.pdf", statement.getPeriodYear(), statement.getPeriodMonth());
        Path file = dir.resolve(filename);
        Files.write(file, pdfBytes);

        return file;
    }
}
