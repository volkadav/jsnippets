package com.norrisjackson.jsnippets.security;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

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
    private final ObjectMapper objectMapper;

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

    public RateLimitingFilter(RateLimiter rateLimiter, ObjectMapper objectMapper) {
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
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
     * Send a rate limit exceeded response with properly JSON-encoded fields.
     */
    private void sendRateLimitResponse(HttpServletResponse response, String path) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setHeader("Retry-After", "60");

        Map<String, Object> errorBody = new LinkedHashMap<>();
        errorBody.put("code", "RATE_LIMIT_EXCEEDED");
        errorBody.put("message", "Too many requests. Please try again later.");
        errorBody.put("timestamp", Instant.now().toString());
        errorBody.put("path", path);

        response.getWriter().write(objectMapper.writeValueAsString(errorBody));
    }

    /**
     * Get a unique identifier for the client.
     * Uses {@code request.getRemoteAddr()} which respects the
     * {@code server.forward-headers-strategy=NATIVE} setting for reverse proxy deployments.
     * This avoids trusting the spoofable X-Forwarded-For header directly.
     */
    private String getClientIdentifier(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}

