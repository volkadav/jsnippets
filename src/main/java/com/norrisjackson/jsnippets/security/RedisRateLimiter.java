package com.norrisjackson.jsnippets.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis-based rate limiter for distributed deployments.
 * Uses Redis to track request counts across multiple application instances.
 *
 * Activated when rate.limit.storage=redis
 */
@Component
@ConditionalOnProperty(name = "rate.limit.storage", havingValue = "redis")
@Slf4j
public class RedisRateLimiter implements RateLimiter {

    private final RedisTemplate<String, String> redisTemplate;

    public RedisRateLimiter(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        log.info("Redis-based rate limiter initialized");
    }

    @Override
    public boolean isAllowed(String key, int maxRequests, int windowSeconds) {
        String redisKey = "rate_limit:" + key;

        try {
            // Increment the counter
            Long count = redisTemplate.opsForValue().increment(redisKey);

            if (count == null) {
                log.warn("Redis increment returned null for key: {}", redisKey);
                return true; // Fail open - allow request if Redis fails
            }

            // Set expiration on first request
            if (count == 1) {
                redisTemplate.expire(redisKey, windowSeconds, TimeUnit.SECONDS);
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

