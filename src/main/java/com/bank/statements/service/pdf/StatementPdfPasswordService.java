package com.bank.statements.service.pdf;

import com.bank.statements.model.Account;
import org.springframework.stereotype.Component;

/**
 * Derives the password required to open a statement PDF, following the
 * common bank e-statement convention of combining something the account
 * holder already knows with something tied to the account itself.
 *
 * Convention used here: first 4 letters of the account holder's name
 * (uppercased) + last 4 digits of the account number.
 * e.g. "Asha Rao" + "SAV-0001-0001" -> "ASHA0001"
 *
 * NOTE: this is deterministic and derivable from data already visible to
 * the account holder - it's meant as a light barrier (matching typical
 * bank practice), not strong encryption. If a stronger/different
 * convention is needed (e.g. date of birth once that field exists), only
 * this class needs to change - nothing else depends on the specific rule.
 */
@Component
public class StatementPdfPasswordService {

    private static final int NAME_PREFIX_LENGTH = 4;
    private static final int ACCOUNT_SUFFIX_LENGTH = 4;

    public String generatePassword(Account account) {
        String namePart = extractNamePrefix(account);
        String accountPart = extractAccountSuffix(account);
        return namePart + accountPart;
    }

    private String extractNamePrefix(Account account) {
        String name = account.getUser().getName().replaceAll("\\s+", "").toUpperCase();
        return name.length() >= NAME_PREFIX_LENGTH
                ? name.substring(0, NAME_PREFIX_LENGTH)
                : name;
    }

    private String extractAccountSuffix(Account account) {
        String digitsOnly = account.getAccountNumber().replaceAll("\\D", "");
        return digitsOnly.length() >= ACCOUNT_SUFFIX_LENGTH
                ? digitsOnly.substring(digitsOnly.length() - ACCOUNT_SUFFIX_LENGTH)
                : digitsOnly;
    }
}
