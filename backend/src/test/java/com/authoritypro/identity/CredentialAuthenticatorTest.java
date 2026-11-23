package com.authoritypro.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.authoritypro.platform.identity.domain.CredentialAuthenticator;
import com.authoritypro.platform.identity.infrastructure.security.SpringPasswordVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class CredentialAuthenticatorTest {
    private static final String HASH = new BCryptPasswordEncoder(12).encode("correct horse battery staple");
    private static final char[] RIGHT = "correct horse battery staple".toCharArray();
    private static final char[] WRONG = "wrong password".toCharArray();

    private final CredentialAuthenticator authenticator = new CredentialAuthenticator(new SpringPasswordVerifier());

    @Test
    void acceptsTheMatchingBcryptPasswordOnly() {
        assertEquals(CredentialAuthenticator.Outcome.AUTHENTICATED,
                authenticator.authenticate(true, "bcrypt", HASH, RIGHT));
        assertEquals(CredentialAuthenticator.Outcome.INVALID_CREDENTIALS,
                authenticator.authenticate(true, "bcrypt", HASH, WRONG));
    }

    @Test
    void checksTheHashEvenWhenTheAccountIsDisabled() {
        assertEquals(CredentialAuthenticator.Outcome.ACCOUNT_DISABLED,
                authenticator.authenticate(false, "bcrypt", HASH, RIGHT));
        assertEquals(CredentialAuthenticator.Outcome.INVALID_CREDENTIALS,
                authenticator.authenticate(false, "bcrypt", HASH, WRONG));
    }

    @Test
    void failsClosedOnMissingOrForeignInputs() {
        assertEquals(CredentialAuthenticator.Outcome.INVALID_CREDENTIALS,
                authenticator.authenticate(true, "bcrypt", HASH, null));
        assertEquals(CredentialAuthenticator.Outcome.INVALID_CREDENTIALS,
                authenticator.authenticate(true, "bcrypt", HASH, new char[0]));
        assertEquals(CredentialAuthenticator.Outcome.INVALID_CREDENTIALS,
                authenticator.authenticate(true, "argon2id", HASH, RIGHT));
        assertEquals(CredentialAuthenticator.Outcome.INVALID_CREDENTIALS,
                authenticator.authenticate(true, "bcrypt", null, RIGHT));
        assertEquals(CredentialAuthenticator.Outcome.INVALID_CREDENTIALS,
                authenticator.authenticate(true, "bcrypt", "plain-text", RIGHT));
    }
}
