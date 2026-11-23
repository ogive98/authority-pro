package com.authoritypro.identity;

import com.authoritypro.platform.identity.domain.LoginThrottle;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginThrottleTest {
    private static final Instant START = Instant.parse("2026-11-23T10:00:00Z");

    @Test
    void fiveFailuresInsideTheWindowBlockTheSource() {
        LoginThrottle throttle = new LoginThrottle();
        for (int attempt = 0; attempt < LoginThrottle.MAX_FAILURES - 1; attempt++) {
            throttle.failed("127.0.0.1", START.plusSeconds(attempt));
            assertFalse(throttle.blocked("127.0.0.1", START.plusSeconds(attempt)));
        }
        throttle.failed("127.0.0.1", START.plusSeconds(10));
        assertTrue(throttle.blocked("127.0.0.1", START.plusSeconds(11)));
        assertFalse(throttle.blocked("127.0.0.2", START.plusSeconds(11)));
    }

    @Test
    void theBlockLiftsOnceTheOldestFailureAgesOut() {
        LoginThrottle throttle = new LoginThrottle();
        for (int attempt = 0; attempt < LoginThrottle.MAX_FAILURES; attempt++) {
            throttle.failed("source-a", START.plusSeconds(attempt));
        }
        assertTrue(throttle.blocked("source-a", START.plusSeconds(30)));
        Instant afterWindow = START.plus(LoginThrottle.WINDOW).plusSeconds(2);
        assertFalse(throttle.blocked("source-a", afterWindow));
    }

    @Test
    void successClearsTheFailureHistory() {
        LoginThrottle throttle = new LoginThrottle();
        for (int attempt = 0; attempt < LoginThrottle.MAX_FAILURES; attempt++) {
            throttle.failed("source-b", START.plusSeconds(attempt));
        }
        assertTrue(throttle.blocked("source-b", START.plusSeconds(10)));
        throttle.succeeded("source-b");
        assertFalse(throttle.blocked("source-b", START.plusSeconds(11)));
    }

    @Test
    void windowBoundaryIsExclusiveOfExactlyFifteenMinutes() {
        LoginThrottle throttle = new LoginThrottle();
        for (int attempt = 0; attempt < LoginThrottle.MAX_FAILURES; attempt++) {
            throttle.failed("source-c", START.plusSeconds(attempt));
        }
        assertTrue(throttle.blocked("source-c", START.plus(Duration.ofMinutes(14))));
        assertTrue(throttle.blocked("source-c", START.plus(Duration.ofMinutes(15)).minusSeconds(1)));
    }

    @Test
    void rejectsMissingInput() {
        LoginThrottle throttle = new LoginThrottle();
        assertThrows(IllegalArgumentException.class, () -> throttle.failed(null, START));
        assertThrows(IllegalArgumentException.class, () -> throttle.blocked("source", null));
    }
}
