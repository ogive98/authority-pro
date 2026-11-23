package com.authoritypro.platform.identity.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/** Application-side normalization mirrors the identity.users CHECK constraint. */
public record LoginName(String value) {
    private static final Pattern FORMAT = Pattern.compile("[a-z0-9][a-z0-9._@-]{2,127}");

    public LoginName {
        if (value == null) throw new IllegalArgumentException("LOGIN_REQUIRED");
        String normalized = value.strip().toLowerCase(Locale.ROOT);
        if (!FORMAT.matcher(normalized).matches()) throw new IllegalArgumentException("LOGIN_INVALID");
        value = normalized;
    }

    public static LoginName of(String raw) {
        return new LoginName(raw);
    }
}
