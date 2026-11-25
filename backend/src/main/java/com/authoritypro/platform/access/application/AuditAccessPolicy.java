package com.authoritypro.platform.access.application;

import java.util.UUID;

/** Scoped decision for the tenant audit read surface; deny-by-default. */
public interface AuditAccessPolicy {

    enum Decision { GRANTED, DENIED }

    Decision decide(UUID userId, UUID tenantId, UUID companyId);
}
