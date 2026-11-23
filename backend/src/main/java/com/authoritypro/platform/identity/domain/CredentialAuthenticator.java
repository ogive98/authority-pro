package com.authoritypro.platform.identity.domain;

import java.util.Objects;

/** Deny-by-default credential decision: the hash is always checked before the account status. */
public final class CredentialAuthenticator {
    public enum Outcome { AUTHENTICATED, INVALID_CREDENTIALS, ACCOUNT_DISABLED }

    private final PasswordVerifier verifier;

    public CredentialAuthenticator(PasswordVerifier verifier) {
        this.verifier = Objects.requireNonNull(verifier, "VERIFIER_REQUIRED");
    }

    public Outcome authenticate(boolean active, String scheme, String encodedHash, char[] rawPassword) {
        if (rawPassword == null || rawPassword.length == 0) return Outcome.INVALID_CREDENTIALS;
        boolean verified = "bcrypt".equals(scheme) && encodedHash != null
                && verifier.matches(rawPassword, encodedHash);
        if (!verified) return Outcome.INVALID_CREDENTIALS;
        return active ? Outcome.AUTHENTICATED : Outcome.ACCOUNT_DISABLED;
    }
}
