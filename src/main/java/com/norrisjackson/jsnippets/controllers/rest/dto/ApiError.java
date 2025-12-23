package com.norrisjackson.jsnippets.controllers.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Standardized API error response format.
 * Used across all REST API endpoints for consistent error handling.
 *
 * @param code      Machine-readable error code (e.g., "AUTH_INVALID_CREDENTIALS")
 * @param message   Human-readable error message
 * @param timestamp When the error occurred
 * @param path      The request path that caused the error (optional)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        String code,
        String message,
        Instant timestamp,
        String path
) {
    /**
     * Create an ApiError with code and message only.
     */
    public static ApiError of(String code, String message) {
        return new ApiError(code, message, Instant.now(), null);
    }

    /**
     * Create an ApiError with code, message, and path.
     */
    public static ApiError of(String code, String message, String path) {
        return new ApiError(code, message, Instant.now(), path);
    }
}

