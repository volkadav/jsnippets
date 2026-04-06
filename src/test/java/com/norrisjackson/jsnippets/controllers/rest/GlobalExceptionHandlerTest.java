package com.norrisjackson.jsnippets.controllers.rest;

import com.norrisjackson.jsnippets.controllers.rest.dto.ApiError;
import com.norrisjackson.jsnippets.controllers.rest.dto.ErrorCodes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/snippets");
    }

    /** Creates a minimal MethodParameter for use in MethodArgumentNotValidException construction. */
    private static MethodParameter makeMethodParameter() throws NoSuchMethodException {
        Method m = String.class.getDeclaredMethod("valueOf", Object.class);
        return new MethodParameter(m, 0);
    }

    // ==================== BadCredentialsException ====================

    @Test
    void handleBadCredentials_returnsUnauthorizedWithCorrectCode() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");

        ResponseEntity<ApiError> response = handler.handleBadCredentials(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(ErrorCodes.AUTH_INVALID_CREDENTIALS);
        assertThat(response.getBody().message()).isEqualTo("Invalid username or password");
        assertThat(response.getBody().path()).isEqualTo("/api/v1/snippets");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    // ==================== AuthenticationException ====================

    @Test
    void handleAuthenticationException_returnsUnauthorizedWithAuthFailedCode() {
        InternalAuthenticationServiceException ex =
                new InternalAuthenticationServiceException("Service unavailable");

        ResponseEntity<ApiError> response = handler.handleAuthenticationException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(ErrorCodes.AUTH_FAILED);
        assertThat(response.getBody().message()).isEqualTo("Authentication failed");
    }

    // ==================== MethodArgumentNotValidException ====================

    @Test
    void handleValidationErrors_returnsBadRequestWithFieldMessages() throws Exception {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "snippetRequest");
        bindingResult.addError(new FieldError("snippetRequest", "contents",
                "Snippet content is required"));

        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(makeMethodParameter(), bindingResult);

        ResponseEntity<ApiError> response = handler.handleValidationErrors(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(ErrorCodes.VALIDATION_ERROR);
        assertThat(response.getBody().message()).contains("Snippet content is required");
    }

    @Test
    void handleValidationErrors_joinsMultipleFieldErrors() throws Exception {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "username", "Username is required"));
        bindingResult.addError(new FieldError("request", "password", "Password is required"));

        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(makeMethodParameter(), bindingResult);

        ResponseEntity<ApiError> response = handler.handleValidationErrors(ex, request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("Username is required");
        assertThat(response.getBody().message()).contains("Password is required");
        assertThat(response.getBody().message()).contains("; ");
    }

    // ==================== MissingServletRequestParameterException ====================

    @Test
    void handleMissingParameter_returnsBadRequestWithParameterName() {
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("contents", "String");

        ResponseEntity<ApiError> response = handler.handleMissingParameter(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(ErrorCodes.MISSING_REQUIRED_FIELD);
        assertThat(response.getBody().message()).isEqualTo("Required parameter 'contents' is missing");
    }

    // ==================== IllegalArgumentException ====================

    @Test
    void handleIllegalArgument_returnsBadRequestWithExceptionMessage() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid snippet ID format");

        ResponseEntity<ApiError> response = handler.handleIllegalArgument(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(ErrorCodes.INVALID_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("Invalid snippet ID format");
    }

    // ==================== Generic Exception ====================

    @Test
    void handleGenericException_returnsInternalServerError() {
        RuntimeException ex = new RuntimeException("Something went wrong");

        ResponseEntity<ApiError> response = handler.handleGenericException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(ErrorCodes.INTERNAL_ERROR);
        assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred");
    }

    @Test
    void handleGenericException_doesNotLeakInternalDetails() {
        RuntimeException ex = new RuntimeException("SQL injection: DROP TABLE users");

        ResponseEntity<ApiError> response = handler.handleGenericException(ex, request);

        assertThat(response.getBody()).isNotNull();
        // Message should be generic, not the exception's internal message
        assertThat(response.getBody().message()).doesNotContain("SQL");
        assertThat(response.getBody().message()).doesNotContain("DROP");
    }

    // ==================== Path Propagation ====================

    @Test
    void allHandlers_propagateRequestPath() {
        request.setRequestURI("/api/v1/auth/login");

        ResponseEntity<ApiError> response = handler.handleBadCredentials(
                new BadCredentialsException("bad"), request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path()).isEqualTo("/api/v1/auth/login");
    }

    // ==================== Timestamp ====================

    @Test
    void allResponses_includeTimestamp() {
        ResponseEntity<ApiError> response = handler.handleIllegalArgument(
                new IllegalArgumentException("test"), request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().timestamp()).isNotNull();
    }
}


