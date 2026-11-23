package com.authoritypro.platform.audit.domain;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bounded, in-memory admission for family C only. Committed A/B records never pass here.
 * Excess attempts are aggregated as visible suppression counters, never queued without bound.
 */
public final class FailedAttemptLimiter {
    public static final int APPROVED_PER_SOURCE_PER_MINUTE = 20;
    public static final int APPROVED_GLOBAL_PER_MINUTE = 600;
    public static final int APPROVED_QUEUE_CAPACITY = 2000;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final int perSource;
    private final int global;
    private final int capacity;
    private final Clock clock;
    private final Map<String, ArrayDeque<Instant>> admittedBySource = new HashMap<>();
    private final ArrayDeque<Instant> admittedGlobal = new ArrayDeque<>();
    private final ArrayDeque<FailedAttempt> queue = new ArrayDeque<>();
    private final Map<String, Long> suppressedBySource = new HashMap<>();
    private long suppressedTotal;

    public FailedAttemptLimiter() {
        this(APPROVED_PER_SOURCE_PER_MINUTE, APPROVED_GLOBAL_PER_MINUTE,
                APPROVED_QUEUE_CAPACITY, Clock.systemUTC());
    }

    public FailedAttemptLimiter(int perSource, int global, int capacity, Clock clock) {
        if (perSource < 1 || global < 1 || capacity < 1 || clock == null) {
            throw new IllegalArgumentException("AUDIT_LIMITER_INVALID");
        }
        this.perSource = perSource;
        this.global = global;
        this.capacity = capacity;
        this.clock = clock;
    }

    public synchronized boolean offer(FailedAttempt attempt) {
        if (attempt == null) throw new IllegalArgumentException("AUDIT_ATTEMPT_INVALID");
        Instant now = clock.instant();
        prune(now);
        ArrayDeque<Instant> source = admittedBySource.computeIfAbsent(
                attempt.sourceClass(), ignored -> new ArrayDeque<>());
        if (source.size() >= perSource || admittedGlobal.size() >= global || queue.size() >= capacity) {
            suppressedBySource.merge(attempt.sourceClass(), 1L, Long::sum);
            suppressedTotal++;
            return false;
        }
        source.addLast(now);
        admittedGlobal.addLast(now);
        queue.addLast(attempt);
        return true;
    }

    public synchronized List<FailedAttempt> drain(int maximum) {
        if (maximum < 1) throw new IllegalArgumentException("AUDIT_ATTEMPT_INVALID");
        List<FailedAttempt> drained = new ArrayList<>(Math.min(maximum, queue.size()));
        while (drained.size() < maximum && !queue.isEmpty()) drained.add(queue.removeFirst());
        return List.copyOf(drained);
    }

    public synchronized Map<String, Long> suppressionCounters() {
        return Collections.unmodifiableMap(new HashMap<>(suppressedBySource));
    }

    public synchronized long suppressedTotal() {
        return suppressedTotal;
    }

    public synchronized int queued() {
        return queue.size();
    }

    private void prune(Instant now) {
        Instant cutoff = now.minus(WINDOW);
        while (!admittedGlobal.isEmpty() && admittedGlobal.peekFirst().isBefore(cutoff)) {
            admittedGlobal.removeFirst();
        }
        for (ArrayDeque<Instant> source : admittedBySource.values()) {
            while (!source.isEmpty() && source.peekFirst().isBefore(cutoff)) source.removeFirst();
        }
    }
}
