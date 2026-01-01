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

/**
 * Rate limiting filter for API endpoints to prevent abuse and brute-force attacks.
 * Uses a sliding window approach with pluggable storage backends (in-memory or Redis).
 *
 * Different rate limits apply to different endpoint types:
 * - Authentication endpoints: Stricter limits (default: 20 req/min)
 * - General API endpoints: More permissive (default: 300 req/min)
 *
 * For production deployments with multiple instances, use Redis-based rate limiting
 * by setting rate.limit.storage=redis
 */
@Component
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimiter rateLimiter;

    // Authentication endpoint limits (stricter)
    @Value("${rate.limit.auth.requests:20}")
    private int authMaxRequests;

    @Value("${rate.limit.auth.window-seconds:60}")
    private int authWindowSeconds;

    // General API endpoint limits (more permissive)
    @Value("${rate.limit.api.requests:300}")
    private int apiMaxRequests;

    @Value("${rate.limit.api.window-seconds:60}")
    private int apiWindowSeconds;

    @Value("${rate.limit.enabled:true}")
    private boolean rateLimitEnabled;

    public RateLimitingFilter(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!rateLimitEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String clientId = getClientIdentifier(request);

        // Determine which rate limit to apply
        if (isAuthenticationEndpoint(path)) {
            // Stricter rate limiting for authentication endpoints
            String key = "auth:" + clientId;
            if (!rateLimiter.isAllowed(key, authMaxRequests, authWindowSeconds)) {
                log.warn("Auth rate limit exceeded for client: {} on path: {}", clientId, path);
                sendRateLimitResponse(response, path);
                return;
            }
        } else if (isApiEndpoint(path)) {
            // General API rate limiting
            String key = "api:" + clientId;
            if (!rateLimiter.isAllowed(key, apiMaxRequests, apiWindowSeconds)) {
                log.warn("API rate limit exceeded for client: {} on path: {}", clientId, path);
                sendRateLimitResponse(response, path);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Check if the path is an authentication endpoint.
     */
    private boolean isAuthenticationEndpoint(String path) {
        return path.contains("/api/v1/auth/login") ||
               path.contains("/api/auth/login") ||
               path.contains("/api/v1/auth/register") ||
               path.contains("/api/auth/register");
    }

    /**
     * Check if the path is an API endpoint (but not authentication).
     */
    private boolean isApiEndpoint(String path) {
        return path.startsWith("/api/") && !isAuthenticationEndpoint(path);
    }

    /**
     * Send a rate limit exceeded response.
     */
    private void sendRateLimitResponse(HttpServletResponse response, String path) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setHeader("Retry-After", "60"); // Hint to client when to retry
        response.getWriter().write(String.format(
                "{\"code\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"Too many requests. Please try again later.\",\"timestamp\":\"%s\",\"path\":\"%s\"}",
                java.time.Instant.now().toString(),
                path
        ));
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
}

