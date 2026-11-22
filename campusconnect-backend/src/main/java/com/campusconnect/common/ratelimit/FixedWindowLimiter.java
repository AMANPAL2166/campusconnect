package com.campusconnect.common.ratelimit;

import com.campusconnect.common.exception.RateLimitedException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixed-window counter per key. A counter lives for `window` after its first hit, then Caffeine drops it.
 * Limits (honest): in-memory and per instance, resets on restart, and allows up to 2x burst around a window edge.
 * Fine for one instance. With 2+ instances the effective limit doubles, and that is the day we consider Redis.
 */
public final class FixedWindowLimiter {

    private final int limit;
    private final Duration window;
    private final Cache<String, AtomicInteger> counters;

    public FixedWindowLimiter(int limit, Duration window, long maxKeys) {
        this.limit = limit;
        this.window = window;
        this.counters = Caffeine.newBuilder().expireAfterWrite(window).maximumSize(maxKeys).build();
    }

    /** Counts this attempt; throws once the limit is passed. */
    public void acquireOrThrow(String key) {
        if (counters.get(key, k -> new AtomicInteger()).incrementAndGet() > limit) {
            throw new RateLimitedException(window.toSeconds());   // upper bound, we don't track the exact remainder
        }
    }

    /** Check only (for failure counting): throws if this key already failed `limit` times. */
    public void failIfExceeded(String key) {
        AtomicInteger c = counters.getIfPresent(key);
        if (c != null && c.get() >= limit) throw new RateLimitedException(window.toSeconds());
    }

    public void recordFailure(String key) {
        counters.get(key, k -> new AtomicInteger()).incrementAndGet();
    }
}