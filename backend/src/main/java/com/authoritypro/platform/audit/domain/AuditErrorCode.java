package com.authoritypro.platform.audit.domain;

/** Safe error categories: no SQL, stack or metadata detail leaves the audit boundary. */
public enum AuditErrorCode {
    AUDIT_INVALID_ACTION,
    AUDIT_SCOPE_MISMATCH,
    AUDIT_METADATA_REJECTED,
    AUDIT_PERSISTENCE_FAILED
}
