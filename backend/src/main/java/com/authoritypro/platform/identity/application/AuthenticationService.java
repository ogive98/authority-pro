package com.authoritypro.platform.identity.application;

import com.authoritypro.platform.identity.domain.CredentialAuthenticator;
import com.authoritypro.platform.identity.domain.LoginName;
import com.authoritypro.platform.identity.domain.LoginThrottle;
import com.authoritypro.platform.identity.domain.PasswordVerifier;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Deny-by-default credential decision. Unknown login, wrong password and disabled account share
 * one refusal; the mismatch is only visible in the throttle bookkeeping.
 */
public final class AuthenticationService {
    // Cost-12 hash of a non-secret phrase: keeps unknown-login timing close to a real check.
    private static final String DUMMY_HASH = "$2a$12$bkQRH5tijgzv4xjrsT0Ubu2xXGTrgqvI3/1vMflauwaU/WQjNyb3G";
    private static final int MAX_PASSWORD_BYTES = 256;
    private static final int MAX_LOGIN_CHARS = 200;
    private static final int MAX_SOURCE_CHARS = 64;

    private final CredentialReader credentials;
    private final PasswordVerifier verifier;
    private final CredentialAuthenticator authenticator;
    private final LoginThrottle throttle;
    private final Clock clock;

    public AuthenticationService(CredentialReader credentials, PasswordVerifier verifier, LoginThrottle throttle,
            Clock clock) {
        this.credentials = Objects.requireNonNull(credentials, "CREDENTIALS_REQUIRED");
        this.verifier = Objects.requireNonNull(verifier, "VERIFIER_REQUIRED");
        this.authenticator = new CredentialAuthenticator(verifier);
        this.throttle = Objects.requireNonNull(throttle, "THROTTLE_REQUIRED");
        this.clock = Objects.requireNonNull(clock, "CLOCK_REQUIRED");
    }

    public record AuthenticatedUser(UUID userId, String displayName) { }

    public AuthenticatedUser authenticate(String rawLogin, String rawPassword, String rawSource) {
        String source = source(rawSource);
        Instant now = clock.instant();
        if (throttle.blocked(source, now)) throw IdentityAccessException.throttled();
        if (!bounded(rawLogin, rawPassword)) {
            throttle.failed(source, now);
            throw IdentityAccessException.refused();
        }
        LoginName login;
        try {
            login = LoginName.of(rawLogin);
        } catch (IllegalArgumentException invalid) {
            throttle.failed(source, now);
            throw IdentityAccessException.refused();
        }
        CredentialReader.Credential credential = credentials.find(login.value()).orElse(null);
        if (credential == null) {
            verifier.matches(rawPassword.toCharArray(), DUMMY_HASH);
            throttle.failed(source, now);
            throw IdentityAccessException.refused();
        }
        CredentialAuthenticator.Outcome outcome = authenticator.authenticate(credential.active(),
                credential.scheme(), credential.passwordHash(), rawPassword.toCharArray());
        if (outcome != CredentialAuthenticator.Outcome.AUTHENTICATED) {
            throttle.failed(source, now);
            throw IdentityAccessException.refused();
        }
        throttle.succeeded(source);
        return new AuthenticatedUser(credential.userId(), credential.displayName());
    }

    private static boolean bounded(String login, String password) {
        if (login == null || password == null) return false;
        return !login.isBlank() && login.length() <= MAX_LOGIN_CHARS
                && !password.isEmpty() && password.getBytes(StandardCharsets.UTF_8).length <= MAX_PASSWORD_BYTES;
    }

    private static String source(String rawSource) {
        if (rawSource == null || rawSource.isBlank()) return "unknown";
        String trimmed = rawSource.strip();
        return trimmed.length() <= MAX_SOURCE_CHARS ? trimmed : trimmed.substring(0, MAX_SOURCE_CHARS);
    }
}
