package com.authoritypro.audit;

import com.authoritypro.platform.audit.domain.AttemptReason;
import com.authoritypro.platform.audit.domain.FailedAttempt;
import com.authoritypro.platform.audit.domain.FailedAttemptLimiter;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FailedAttemptLimiterTest {
    private static FailedAttempt attempt(String source) {
        return new FailedAttempt(AttemptReason.INVALID_CREDENTIALS, null, source);
    }

    @Test
    void twentyPerSourceThenSuppressionAndOtherClassesStillPass() {
        FailedAttemptLimiter limiter = new FailedAttemptLimiter();
        for (int i = 0; i < 20; i++) assertTrue(limiter.offer(attempt("login")));
        assertFalse(limiter.offer(attempt("login")));
        assertTrue(limiter.offer(attempt("session")));
        assertEquals(1L, limiter.suppressedTotal());
        assertEquals(1L, limiter.suppressionCounters().get("login"));
        assertEquals(20, limiter.drain(100).stream().filter(a -> a.sourceClass().equals("login")).toList().size());
    }

    @Test
    void globalLimitSuppressesEvenAcrossClasses() {
        FailedAttemptLimiter limiter = new FailedAttemptLimiter(100, 5, 100, Clock.systemUTC());
        for (int i = 0; i < 5; i++) assertTrue(limiter.offer(attempt("class-" + i)));
        assertFalse(limiter.offer(attempt("class-9")));
        assertEquals(1L, limiter.suppressedTotal());
    }

    @Test
    void queueIsBoundedAndDrainIsBounded() {
        FailedAttemptLimiter limiter = new FailedAttemptLimiter(100, 100, 2, Clock.systemUTC());
        assertTrue(limiter.offer(attempt("login")));
        assertTrue(limiter.offer(attempt("login")));
        assertFalse(limiter.offer(attempt("login")));
        assertEquals(2, limiter.queued());
        assertEquals(1, limiter.drain(1).size());
        assertEquals(1, limiter.queued());
    }

    @Test
    void windowIsOneMinuteAndSuppressionIsVisible() {
        MutableClock clock = new MutableClock();
        FailedAttemptLimiter limiter = new FailedAttemptLimiter(2, 2, 10, clock);
        assertTrue(limiter.offer(attempt("login")));
        assertTrue(limiter.offer(attempt("login")));
        assertFalse(limiter.offer(attempt("login")));
        clock.advance(Duration.ofSeconds(61));
        assertTrue(limiter.offer(attempt("login")));
        assertEquals(1L, limiter.suppressedTotal());
    }

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-21T10:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
