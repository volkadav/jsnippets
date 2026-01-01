package com.norrisjackson.jsnippets.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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

        buckets.compute(key, (k, bucket) -> {
            if (bucket == null || bucket.windowStart < windowStart) {
                // Create new bucket or reset expired bucket
                return new RateLimitBucket(currentTime, new AtomicInteger(1));
            }
            bucket.count.incrementAndGet();
            return bucket;
        });

        RateLimitBucket bucket = buckets.get(key);
        boolean allowed = bucket.count.get() <= maxRequests;

        // Cleanup old buckets periodically
        if (buckets.size() > CLEANUP_THRESHOLD) {
            cleanup(windowStart);
        }

        return allowed;
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
     * Inner class to hold rate limit bucket data.
     */
    private static class RateLimitBucket {
        final long windowStart;
        final AtomicInteger count;

        RateLimitBucket(long windowStart, AtomicInteger count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}

