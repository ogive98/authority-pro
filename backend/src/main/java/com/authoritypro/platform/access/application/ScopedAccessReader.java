package com.authoritypro.platform.access.application;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolves a selector against a fresh active membership and the pinned authorization revision.
 * The HTTP selector is an input only; labels come from the caller's own member-visible rows.
 */
public interface ScopedAccessReader {

    Optional<ScopedAccess> read(UUID userId, UUID tenantId, UUID companyId);
}
