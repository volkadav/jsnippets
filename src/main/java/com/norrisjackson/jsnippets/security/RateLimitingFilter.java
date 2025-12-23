package com.norrisjackson.jsnippets.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiting filter for authentication endpoints to prevent brute-force attacks.
 * Uses a sliding window approach with in-memory storage.
 *
 * For production deployments with multiple instances, consider using Redis-based rate limiting.
 */
@Component
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();

    @Value("${rate.limit.requests:10}")
    private int maxRequests;

    @Value("${rate.limit.window-seconds:60}")
    private int windowSeconds;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Only rate limit authentication endpoints
        if (path.contains("/api/v1/auth/login") || path.contains("/api/auth/login")) {
            String clientKey = getClientIdentifier(request);

            if (!isAllowed(clientKey)) {
                log.warn("Rate limit exceeded for client: {}", clientKey);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Too many requests. Please try again later.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Get a unique identifier for the client.
     * Uses X-Forwarded-For header if present (for reverse proxy setups), otherwise uses remote address.
     */
    private String getClientIdentifier(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Take the first IP in the chain (original client)
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Check if the request is allowed based on rate limiting rules.
     */
    private boolean isAllowed(String clientKey) {
        long currentTime = System.currentTimeMillis();
        long windowStart = currentTime - (windowSeconds * 1000L);

        buckets.compute(clientKey, (key, bucket) -> {
            if (bucket == null || bucket.windowStart < windowStart) {
                // Create new bucket or reset expired bucket
                return new RateLimitBucket(currentTime, new AtomicInteger(1));
            }
            bucket.count.incrementAndGet();
            return bucket;
        });

        RateLimitBucket bucket = buckets.get(clientKey);
        boolean allowed = bucket.count.get() <= maxRequests;

        // Cleanup old buckets periodically (simple approach)
        if (buckets.size() > 10000) {
            cleanupOldBuckets(windowStart);
        }

        return allowed;
    }

    /**
     * Remove expired buckets to prevent memory leaks.
     */
    private void cleanupOldBuckets(long windowStart) {
        buckets.entrySet().removeIf(entry -> entry.getValue().windowStart < windowStart);
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

