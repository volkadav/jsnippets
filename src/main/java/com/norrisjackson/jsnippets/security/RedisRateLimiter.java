package com.norrisjackson.jsnippets.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * Redis-based rate limiter for distributed deployments.
 * Uses Redis to track request counts across multiple application instances.
 * Increment and expire are performed atomically via a Lua script to prevent
 * keys from leaking if the process crashes between the two operations.
 *
 * Activated when rate.limit.storage=redis
 */
@Component
@ConditionalOnProperty(name = "rate.limit.storage", havingValue = "redis")
@Slf4j
public class RedisRateLimiter implements RateLimiter {

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Lua script that atomically increments a counter and sets expiration on the first request.
     * KEYS[1] = rate limit key
     * ARGV[1] = window expiration in seconds
     * Returns the current count after increment.
     */
    private static final RedisScript<Long> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>(
            "local count = redis.call('INCR', KEYS[1]) " +
            "if count == 1 then " +
            "  redis.call('EXPIRE', KEYS[1], ARGV[1]) " +
            "end " +
            "return count",
            Long.class
    );

    public RedisRateLimiter(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        log.info("Redis-based rate limiter initialized");
    }

    @Override
    public boolean isAllowed(String key, int maxRequests, int windowSeconds) {
        String redisKey = "rate_limit:" + key;

        try {
            Long count = redisTemplate.execute(
                    RATE_LIMIT_SCRIPT,
                    Collections.singletonList(redisKey),
                    String.valueOf(windowSeconds)
            );

            if (count == null) {
                log.warn("Redis rate limit script returned null for key: {}", redisKey);
                return true; // Fail open - allow request if Redis fails
            }

            boolean allowed = count <= maxRequests;

            if (!allowed) {
                log.debug("Rate limit exceeded for key: {} (count: {}, max: {})", key, count, maxRequests);
            }

            return allowed;

        } catch (Exception e) {
            log.error("Error checking rate limit in Redis for key: {}", key, e);
            return true; // Fail open - allow request if Redis fails
        }
    }

    @Override
    public void cleanup() {
        // Redis automatically cleans up expired keys, no action needed
        log.debug("Redis rate limiter cleanup called (no-op - Redis handles expiration)");
    }
}

