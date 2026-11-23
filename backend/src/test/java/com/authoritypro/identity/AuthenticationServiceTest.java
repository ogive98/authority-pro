package com.authoritypro.identity;

import com.authoritypro.platform.identity.application.AuthenticationService;
import com.authoritypro.platform.identity.application.CredentialReader;
import com.authoritypro.platform.identity.application.IdentityAccessException;
import com.authoritypro.platform.identity.domain.LoginThrottle;
import com.authoritypro.platform.identity.domain.PasswordVerifier;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthenticationServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-23T10:00:00Z");
    private static final UUID USER = UUID.randomUUID();
    private static final String PASSWORD = "correct-password";

    private final PasswordVerifier verifier = (raw, encoded) ->
            encoded != null && PASSWORD.equals(new String(raw));

    private AuthenticationService service(CredentialReader reader) {
        return new AuthenticationService(reader, verifier, new LoginThrottle(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static CredentialReader reader(boolean active, String scheme) {
        return login -> "operator".equals(login)
                ? Optional.of(new CredentialReader.Credential(USER, "Operator", active,
                    "stored-hash", scheme))
                : Optional.empty();
    }

    @Test
    void authenticatesTheMatchingCredential() {
        AuthenticationService.AuthenticatedUser user = service(reader(true, "bcrypt"))
                .authenticate("Operator", PASSWORD, "127.0.0.1");
        assertEquals(USER, user.userId());
        assertEquals("Operator", user.displayName());
    }

    @Test
    void unknownUserAndWrongPasswordShareTheSameRefusal() {
        AuthenticationService service = service(reader(true, "bcrypt"));
        assertEquals(IdentityAccessException.Kind.AUTHENTICATION_REFUSED,
                assertThrows(IdentityAccessException.class,
                        () -> service.authenticate("unknown", PASSWORD, "source")).kind());
        assertEquals(IdentityAccessException.Kind.AUTHENTICATION_REFUSED,
                assertThrows(IdentityAccessException.class,
                        () -> service.authenticate("operator", "wrong-password", "source")).kind());
    }

    @Test
    void disabledAccountIsRefusedButKeepTheSessionPolicyDecisionsInsideTheDomain() {
        assertEquals(IdentityAccessException.Kind.AUTHENTICATION_REFUSED,
                assertThrows(IdentityAccessException.class,
                        () -> service(reader(false, "bcrypt")).authenticate("operator", PASSWORD, "source")).kind());
    }

    @Test
    void nonBcryptSchemeIsRefusedEvenWithTheRightPassword() {
        assertEquals(IdentityAccessException.Kind.AUTHENTICATION_REFUSED,
                assertThrows(IdentityAccessException.class,
                        () -> service(reader(true, "sha256")).authenticate("operator", PASSWORD, "source")).kind());
    }

    @Test
    void fiveFailuresThrottleTheSameSource() {
        AuthenticationService service = service(reader(true, "bcrypt"));
        for (int attempt = 0; attempt < LoginThrottle.MAX_FAILURES; attempt++) {
            assertThrows(IdentityAccessException.class,
                    () -> service.authenticate("operator", "wrong", "10.0.0.1"));
        }
        IdentityAccessException throttled = assertThrows(IdentityAccessException.class,
                () -> service.authenticate("operator", PASSWORD, "10.0.0.1"));
        assertEquals(IdentityAccessException.Kind.AUTHENTICATION_THROTTLED, throttled.kind());
    }

    @Test
    void oversizedInputsAreRefusedBeforeTheDirectoryIsRead() {
        CredentialReader neverRead = login -> {
            throw new AssertionError("directory must not be read for oversized input");
        };
        AuthenticationService service = service(neverRead);
        assertEquals(IdentityAccessException.Kind.AUTHENTICATION_REFUSED,
                assertThrows(IdentityAccessException.class,
                        () -> service.authenticate("operator", "x".repeat(257), "source")).kind());
    }

    @Test
    void storeOutagePropagatesAsUnavailable() {
        CredentialReader failing = login -> {
            throw IdentityAccessException.storeUnavailable(new IllegalStateException("down"));
        };
        assertEquals(IdentityAccessException.Kind.STORE_UNAVAILABLE,
                assertThrows(IdentityAccessException.class,
                        () -> service(failing).authenticate("operator", PASSWORD, "source")).kind());
    }
}
