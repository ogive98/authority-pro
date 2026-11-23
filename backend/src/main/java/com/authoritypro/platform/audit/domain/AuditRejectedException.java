package com.authoritypro.platform.audit.domain;

public final class AuditRejectedException extends RuntimeException {
    private final AuditErrorCode code;

    public AuditRejectedException(AuditErrorCode code) {
        super(code.name());
        this.code = code;
    }

    public AuditRejectedException(AuditErrorCode code, Throwable cause) {
        super(code.name(), cause);
        this.code = code;
    }

    public AuditErrorCode code() {
        return code;
    }
}
