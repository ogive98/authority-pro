package com.authoritypro.platform.identity.infrastructure.security;

import com.authoritypro.platform.identity.domain.PasswordVerifier;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** BCrypt strength 12 per D1; the encoder also enforces the stored hash format. */
public final class SpringPasswordVerifier implements PasswordVerifier {
    private static final int STRENGTH = 12;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(STRENGTH);

    @Override
    public boolean matches(char[] rawPassword, String encodedHash) {
        if (rawPassword == null || rawPassword.length == 0 || encodedHash == null) return false;
        return encoder.matches(new String(rawPassword), encodedHash);
    }
}
