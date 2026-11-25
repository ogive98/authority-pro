package com.authoritypro.platform.audit.application;

/** The audit read store or its scoped transaction could not be used. */
public final class AuditReadUnavailableException extends RuntimeException {

    public AuditReadUnavailableException() {
        super("AUDIT_READ_UNAVAILABLE");
    }
}
