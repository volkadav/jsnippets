package com.norrisjackson.jsnippets.controllers.rest;

import com.norrisjackson.jsnippets.controllers.rest.dto.ApiError;
import com.norrisjackson.jsnippets.controllers.rest.dto.ErrorCodes;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Global exception handler for REST API endpoints.
 * Converts exceptions to standardized ApiError responses.
 */
@RestControllerAdvice(basePackages = "com.norrisjackson.jsnippets.controllers.rest")
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle authentication failures (bad credentials)
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Authentication failed for request to {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of(
                        ErrorCodes.AUTH_INVALID_CREDENTIALS,
                        "Invalid username or password",
                        request.getRequestURI()
                ));
    }

    /**
     * Handle other authentication exceptions
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication error for request to {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of(
                        ErrorCodes.AUTH_FAILED,
                        "Authentication failed",
                        request.getRequestURI()
                ));
    }

    /**
     * Handle validation errors from @Valid annotations
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.debug("Validation error for request to {}: {}", request.getRequestURI(), message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(
                        ErrorCodes.VALIDATION_ERROR,
                        message,
                        request.getRequestURI()
                ));
    }

    /**
     * Handle missing request parameters
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(MissingServletRequestParameterException ex, HttpServletRequest request) {
        String message = String.format("Required parameter '%s' is missing", ex.getParameterName());
        log.debug("Missing parameter for request to {}: {}", request.getRequestURI(), message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(
                        ErrorCodes.MISSING_REQUIRED_FIELD,
                        message,
                        request.getRequestURI()
                ));
    }

    /**
     * Handle illegal argument exceptions (invalid input)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.debug("Invalid argument for request to {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(
                        ErrorCodes.INVALID_REQUEST,
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    /**
     * Catch-all handler for unexpected exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error for request to {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of(
                        ErrorCodes.INTERNAL_ERROR,
                        "An unexpected error occurred",
                        request.getRequestURI()
                ));
    }
}

