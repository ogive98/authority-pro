package com.authoritypro.platform.identity.domain;

import java.time.Duration;
import java.time.Instant;

/** Owner-approved L05 session policy (D2): 30 minutes idle, 8 hours absolute, 3 concurrent sessions. */
public record SessionPolicy(Duration idleTimeout, Duration absoluteLifetime, int maxConcurrent) {
    public SessionPolicy {
        if (idleTimeout == null || idleTimeout.isZero() || idleTimeout.isNegative()) {
            throw new IllegalArgumentException("IDLE_TIMEOUT_INVALID");
        }
        if (absoluteLifetime == null || absoluteLifetime.compareTo(idleTimeout) <= 0) {
            throw new IllegalArgumentException("ABSOLUTE_LIFETIME_INVALID");
        }
        if (maxConcurrent < 1 || maxConcurrent > 10) throw new IllegalArgumentException("MAX_CONCURRENT_INVALID");
    }

    public static SessionPolicy standard() {
        return new SessionPolicy(Duration.ofMinutes(30), Duration.ofHours(8), 3);
    }

    public Instant idleDeadline(Instant lastSeen) {
        return lastSeen.plus(idleTimeout);
    }

    public Instant absoluteDeadline(Instant created) {
        return created.plus(absoluteLifetime);
    }

    public boolean expired(Instant created, Instant lastSeen, Instant now) {
        return !now.isBefore(idleDeadline(lastSeen)) || !now.isBefore(absoluteDeadline(created));
    }
}
