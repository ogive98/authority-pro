package com.authoritypro.platform.identity.domain;

/** Port for the approved password hash (BCrypt strength 12, D1); implementations live in infrastructure. */
@FunctionalInterface
public interface PasswordVerifier {
    boolean matches(char[] rawPassword, String encodedHash);
}
