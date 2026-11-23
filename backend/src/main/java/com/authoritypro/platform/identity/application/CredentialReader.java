package com.authoritypro.platform.identity.application;

import java.util.Optional;
import java.util.UUID;

/** Login-plane port: the adapter resolves the normalized login without tenant context. */
public interface CredentialReader {
    record Credential(UUID userId, String displayName, boolean active, String passwordHash, String scheme) { }

    Optional<Credential> find(String normalizedLogin);
}
