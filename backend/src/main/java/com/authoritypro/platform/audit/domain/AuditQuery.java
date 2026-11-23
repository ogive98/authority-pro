package com.authoritypro.platform.audit.domain;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/** Bounded filters only: no free-text, wildcard actor search or unbounded export. */
public record AuditQuery(
        AuditAction action,
        String targetType,
        String targetId,
        UUID actorId,
        Instant from,
        Instant to,
        UUID companyId,
        Instant cursorOccurredAt,
        UUID cursorRecordId,
        int pageSize) {

    public static final int MAX_PAGE_SIZE = 100;
    private static final Pattern TARGET = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:@-]{0,127}");
    private static final Set<String> TARGET_TYPES = Set.of(
            "operator", "session", "subject", "membership", "grant", "company", "settings",
            "module", "configuration", "retention");

    public AuditQuery {
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("AUDIT_PAGE_SIZE_INVALID");
        }
        if ((targetType == null) != (targetId == null)) {
            throw new IllegalArgumentException("AUDIT_TARGET_INCOMPLETE");
        }
        if (targetType != null && (!TARGET_TYPES.contains(targetType) || !TARGET.matcher(targetId).matches())) {
            throw new IllegalArgumentException("AUDIT_TARGET_INVALID");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("AUDIT_RANGE_INVALID");
        }
        if ((cursorOccurredAt == null) != (cursorRecordId == null)) {
            throw new IllegalArgumentException("AUDIT_CURSOR_INCOMPLETE");
        }
        if (action != null && targetType != null && !action.targetType().equals(targetType)) {
            throw new IllegalArgumentException("AUDIT_TARGET_ACTION_MISMATCH");
        }
    }

    public static AuditQuery firstPage(int pageSize) {
        return new AuditQuery(null, null, null, null, null, null, null, null, null, pageSize);
    }
}
