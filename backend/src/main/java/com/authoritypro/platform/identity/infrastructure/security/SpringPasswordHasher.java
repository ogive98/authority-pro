package com.authoritypro.platform.identity.infrastructure.security;

import com.authoritypro.platform.identity.application.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Approved v1 hashing (D1): BCrypt work factor 12, scheme recorded by the caller. */
public final class SpringPasswordHasher implements PasswordHasher {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    @Override
    public String encode(char[] rawPassword) {
        if (rawPassword == null || rawPassword.length == 0) throw new IllegalArgumentException("PASSWORD_REQUIRED");
        return encoder.encode(new String(rawPassword));
    }
}
