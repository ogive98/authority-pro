package com.authoritypro.platform.audit.domain;

import java.util.UUID;
import java.util.regex.Pattern;

/** Minimal failed-attempt record: bounded source class and reason only. */
public record FailedAttempt(AttemptReason reason, UUID correlationId, String sourceClass) {
    private static final Pattern SOURCE = Pattern.compile("[A-Za-z][A-Za-z0-9._-]{0,31}");

    public FailedAttempt {
        if (reason == null || sourceClass == null || !SOURCE.matcher(sourceClass).matches()) {
            throw new IllegalArgumentException("AUDIT_ATTEMPT_INVALID");
        }
    }
}
