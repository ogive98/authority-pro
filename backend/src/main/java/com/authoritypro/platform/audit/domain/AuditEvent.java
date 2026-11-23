package com.authoritypro.platform.audit.domain;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * A critical record is written only with verified scope. Family A also requires an active
 * membership checked by the store inside the caller's transaction.
 */
public record AuditEvent(
        AuditAction action,
        UUID actorId,
        AuditActorKind actorKind,
        String targetId,
        UUID operationId,
        UUID correlationId,
        UUID tenantId,
        UUID companyId,
        UUID siteId,
        AuditMetadata metadata) {

    public static final int SCHEMA_VERSION = 1;
    private static final Pattern TARGET_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:@-]{0,127}");

    public AuditEvent {
        if (action == null || actorId == null || actorKind == null || metadata == null
                || targetId == null || !TARGET_ID.matcher(targetId).matches()) {
            throw new AuditRejectedException(AuditErrorCode.AUDIT_INVALID_ACTION);
        }
        AuditFamily family = tenantId == null ? AuditFamily.CONTROL_PLANE : AuditFamily.TENANT;
        if (!action.permits(family)) {
            throw new AuditRejectedException(AuditErrorCode.AUDIT_SCOPE_MISMATCH);
        }
        if (companyId != null && tenantId == null) {
            throw new AuditRejectedException(AuditErrorCode.AUDIT_SCOPE_MISMATCH);
        }
        if (siteId != null && companyId == null) {
            throw new AuditRejectedException(AuditErrorCode.AUDIT_SCOPE_MISMATCH);
        }
        if (tenantId == null && (companyId != null || siteId != null)) {
            throw new AuditRejectedException(AuditErrorCode.AUDIT_SCOPE_MISMATCH);
        }
    }

    public AuditFamily family() {
        return tenantId == null ? AuditFamily.CONTROL_PLANE : AuditFamily.TENANT;
    }

    private static String targetTypeOf(AuditAction action) {
        return action.targetType();
    }
}
