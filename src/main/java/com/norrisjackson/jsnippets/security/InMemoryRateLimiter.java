package com.norrisjackson.jsnippets.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * In-memory rate limiter for single-instance deployments.
 * Uses local ConcurrentHashMap to track request counts.
 *
 * Activated when rate.limit.storage=memory (default)
 */
@Component
@ConditionalOnProperty(name = "rate.limit.storage", havingValue = "memory", matchIfMissing = true)
@Slf4j
public class InMemoryRateLimiter implements RateLimiter {

    private final Map<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();
    private static final int CLEANUP_THRESHOLD = 10000;

    public InMemoryRateLimiter() {
        log.info("In-memory rate limiter initialized (single-instance mode)");
    }

    @Override
    public boolean isAllowed(String key, int maxRequests, int windowSeconds) {
        long currentTime = System.currentTimeMillis();
        long windowStart = currentTime - (windowSeconds * 1000L);

        // Determine the result atomically inside compute to avoid TOCTOU race
        AtomicBoolean allowed = new AtomicBoolean(true);

        buckets.compute(key, (k, bucket) -> {
            if (bucket == null || bucket.windowStart < windowStart) {
                // New window — first request is always allowed (assuming maxRequests >= 1)
                allowed.set(1 <= maxRequests);
                return new RateLimitBucket(currentTime, 1);
            }
            int newCount = bucket.count + 1;
            allowed.set(newCount <= maxRequests);
            return new RateLimitBucket(bucket.windowStart, newCount);
        });

        // Cleanup old buckets periodically
        if (buckets.size() > CLEANUP_THRESHOLD) {
            cleanup(windowStart);
        }

        return allowed.get();
    }

    @Override
    public void cleanup() {
        cleanup(System.currentTimeMillis());
    }

    /**
     * Remove expired buckets to prevent memory leaks.
     *
     * @param windowStart the cutoff time before which buckets are expired
     */
    private void cleanup(long windowStart) {
        int beforeSize = buckets.size();
        buckets.entrySet().removeIf(entry -> entry.getValue().windowStart < windowStart);
        int afterSize = buckets.size();

        if (beforeSize > afterSize) {
            log.debug("Cleaned up {} expired rate limit buckets", beforeSize - afterSize);
        }
    }

    /**
     * Immutable bucket holding window start time and request count.
     * Immutability is safe here because all mutations happen inside
     * {@link ConcurrentHashMap#compute}, which is atomic per key.
     */
    private static class RateLimitBucket {
        final long windowStart;
        final int count;

        RateLimitBucket(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}

