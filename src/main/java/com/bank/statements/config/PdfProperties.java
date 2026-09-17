package com.bank.statements.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "statement.pdf")
@Getter
@Setter
public class PdfProperties {

    /** Directory where generated PDFs for GENERATED (final) statements are cached. */
    private String storagePath;

    /** Unlocks full permissions regardless of the per-statement user password. */
    private String ownerPassword;
}
