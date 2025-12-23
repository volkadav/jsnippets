package com.norrisjackson.jsnippets.controllers.rest.dto;

/**
 * Constants for machine-readable API error codes.
 * These codes allow clients to programmatically handle specific error conditions.
 */
public final class ErrorCodes {

    private ErrorCodes() {
        // Prevent instantiation
    }

    // Authentication errors
    public static final String AUTH_INVALID_CREDENTIALS = "AUTH_INVALID_CREDENTIALS";
    public static final String AUTH_TOKEN_EXPIRED = "AUTH_TOKEN_EXPIRED";
    public static final String AUTH_TOKEN_INVALID = "AUTH_TOKEN_INVALID";
    public static final String AUTH_TOKEN_MISSING = "AUTH_TOKEN_MISSING";
    public static final String AUTH_FAILED = "AUTH_FAILED";

    // Authorization errors
    public static final String ACCESS_DENIED = "ACCESS_DENIED";
    public static final String FORBIDDEN = "FORBIDDEN";

    // Resource errors
    public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    public static final String SNIPPET_NOT_FOUND = "SNIPPET_NOT_FOUND";
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";

    // Validation errors
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String INVALID_REQUEST = "INVALID_REQUEST";
    public static final String MISSING_REQUIRED_FIELD = "MISSING_REQUIRED_FIELD";

    // Rate limiting
    public static final String RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";

    // Server errors
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    public static final String SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";
}

