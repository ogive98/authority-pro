package com.authoritypro.platform.audit.domain;

import java.time.Instant;
import java.util.UUID;

/** Safe read projection. Metadata is the validated canonical allowlist JSON, never raw rows. */
public record AuditRecordView(
        UUID recordId,
        Instant occurredAt,
        String actionCode,
        int actionVersion,
        UUID actorId,
        AuditActorKind actorKind,
        String targetType,
        String targetId,
        UUID operationId,
        UUID correlationId,
        int schemaVersion,
        String metadata,
        String outcome,
        UUID tenantId,
        UUID companyId,
        UUID siteId) {
}
