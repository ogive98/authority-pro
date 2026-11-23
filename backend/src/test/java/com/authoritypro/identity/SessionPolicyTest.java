package com.authoritypro.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.authoritypro.platform.identity.domain.SessionPolicy;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SessionPolicyTest {
    private static final Instant CREATED = Instant.parse("2026-11-21T08:00:00Z");

    @Test
    void standardPolicyMatchesTheOwnerDecision() {
        SessionPolicy policy = SessionPolicy.standard();
        assertEquals(Duration.ofMinutes(30), policy.idleTimeout());
        assertEquals(Duration.ofHours(8), policy.absoluteLifetime());
        assertEquals(3, policy.maxConcurrent());
    }

    @Test
    void idleExpiryIsInclusiveAtTheDeadline() {
        SessionPolicy policy = SessionPolicy.standard();
        Instant lastSeen = CREATED.plus(Duration.ofHours(2));
        assertFalse(policy.expired(CREATED, lastSeen, lastSeen.plus(Duration.ofMinutes(29)).plusSeconds(59)));
        assertTrue(policy.expired(CREATED, lastSeen, lastSeen.plus(Duration.ofMinutes(30))));
    }

    @Test
    void absoluteExpiryIsInclusiveEvenWithRecentActivity() {
        SessionPolicy policy = SessionPolicy.standard();
        Instant lastSeen = CREATED.plus(Duration.ofHours(7).plusMinutes(50));
        assertFalse(policy.expired(CREATED, lastSeen, CREATED.plus(Duration.ofHours(7)).plus(Duration.ofMinutes(59))));
        assertTrue(policy.expired(CREATED, lastSeen, CREATED.plus(Duration.ofHours(8))));
    }

    @Test
    void rejectsIncoherentBounds() {
        assertThrows(IllegalArgumentException.class,
                () -> new SessionPolicy(Duration.ZERO, Duration.ofHours(8), 3));
        assertThrows(IllegalArgumentException.class,
                () -> new SessionPolicy(Duration.ofHours(1), Duration.ofHours(1), 3));
        assertThrows(IllegalArgumentException.class,
                () -> new SessionPolicy(Duration.ofMinutes(30), Duration.ofHours(8), 0));
        assertThrows(IllegalArgumentException.class,
                () -> new SessionPolicy(Duration.ofMinutes(30), Duration.ofHours(8), 11));
    }
}
