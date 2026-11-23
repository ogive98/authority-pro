package com.authoritypro.platform.audit.domain;

/** Approved reason codes; no username, secret, token, IP address or request body. */
public enum AttemptReason {
    INVALID_CREDENTIALS("invalid_credentials"),
    ACCOUNT_LOCKED("account_locked"),
    SESSION_EXPIRED("session_expired"),
    RATE_LIMITED("rate_limited"),
    CSRF_REJECTED("csrf_rejected"),
    AUTHORIZATION_DENIED("authorization_denied");

    private final String code;

    AttemptReason(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
