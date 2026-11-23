package com.authoritypro.platform.identity.application;

/** Encodes a raw password with the approved adaptive scheme before persistence. */
public interface PasswordHasher {
    String encode(char[] rawPassword);
}
