package com.norrisjackson.jsnippets.security;
/**
 * Interface for rate limiting implementations.
 * Allows for different storage backends (in-memory, Redis, etc.)
 */
public interface RateLimiter {
    /**
     * Check if a request is allowed based on rate limiting rules.
     *
     * @param key unique identifier for the client (e.g., IP address)
     * @param maxRequests maximum number of requests allowed in the window
     * @param windowSeconds time window in seconds
     * @return true if request is allowed, false if rate limit exceeded
     */
    boolean isAllowed(String key, int maxRequests, int windowSeconds);
    /**
     * Cleanup expired entries (for in-memory implementations).
     * No-op for Redis-based implementations.
     */
    void cleanup();
}
