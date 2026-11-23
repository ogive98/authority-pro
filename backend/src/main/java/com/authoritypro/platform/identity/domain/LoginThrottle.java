package com.authoritypro.platform.identity.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bounded per-source admission for authentication attempts: five failures inside a fifteen-minute
 * window block the source until the oldest failure ages out. Account-level locking would need a
 * durable store and is deliberately deferred (the approved attempt path is per source).
 */
public final class LoginThrottle {
    public static final int MAX_FAILURES = 5;
    public static final Duration WINDOW = Duration.ofMinutes(15);
    private static final int MAX_SOURCES = 1024;

    private final Map<String, ArrayDeque<Instant>> failures = new LinkedHashMap<>(64, 0.75f, true);

    public synchronized boolean blocked(String source, Instant now) {
        require(source, now);
        prune(source, now);
        ArrayDeque<Instant> entries = failures.get(source);
        return entries != null && entries.size() >= MAX_FAILURES;
    }

    public synchronized void failed(String source, Instant now) {
        require(source, now);
        prune(source, now);
        failures.computeIfAbsent(source, ignored -> new ArrayDeque<>(MAX_FAILURES)).addLast(now);
        evictOldestSource();
    }

    public synchronized void succeeded(String source) {
        if (source == null) throw new IllegalArgumentException("THROTTLE_INPUT_INVALID");
        failures.remove(source);
    }

    private void prune(String source, Instant now) {
        ArrayDeque<Instant> entries = failures.get(source);
        if (entries == null) return;
        Instant cutoff = now.minus(WINDOW);
        while (!entries.isEmpty() && !entries.peekFirst().isAfter(cutoff)) entries.removeFirst();
        if (entries.isEmpty()) failures.remove(source);
    }

    private void evictOldestSource() {
        while (failures.size() > MAX_SOURCES) {
            Iterator<String> oldest = failures.keySet().iterator();
            oldest.next();
            oldest.remove();
        }
    }

    private static void require(String source, Instant now) {
        if (source == null || source.isEmpty() || now == null) {
            throw new IllegalArgumentException("THROTTLE_INPUT_INVALID");
        }
    }
}
