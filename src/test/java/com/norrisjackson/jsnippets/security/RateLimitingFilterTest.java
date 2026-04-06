package com.norrisjackson.jsnippets.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    @Mock
    private RateLimiter rateLimiter;

    @Mock
    private FilterChain filterChain;

    private RateLimitingFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        filter = new RateLimitingFilter(rateLimiter, objectMapper);
        ReflectionTestUtils.setField(filter, "rateLimitEnabled", true);
        ReflectionTestUtils.setField(filter, "authMaxRequests", 20);
        ReflectionTestUtils.setField(filter, "authWindowSeconds", 60);
        ReflectionTestUtils.setField(filter, "apiMaxRequests", 300);
        ReflectionTestUtils.setField(filter, "apiWindowSeconds", 60);

        request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");
        response = new MockHttpServletResponse();
    }

    // ==================== Disabled Rate Limiting ====================

    @Test
    void doFilter_whenDisabled_passesThrough() throws Exception {
        ReflectionTestUtils.setField(filter, "rateLimitEnabled", false);
        request.setRequestURI("/api/v1/snippets");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(rateLimiter);
    }

    // ==================== Auth Endpoint Rate Limiting ====================

    @Test
    void doFilter_authEndpoint_withinLimit_passesThrough() throws Exception {
        request.setRequestURI("/api/v1/auth/login");
        when(rateLimiter.isAllowed(eq("auth:192.168.1.100"), eq(20), eq(60))).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void doFilter_authEndpoint_exceedsLimit_returns429() throws Exception {
        request.setRequestURI("/api/v1/auth/login");
        when(rateLimiter.isAllowed(eq("auth:192.168.1.100"), eq(20), eq(60))).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getHeader("Retry-After")).isEqualTo("60");
    }

    @Test
    void doFilter_authRegisterEndpoint_usesAuthLimits() throws Exception {
        request.setRequestURI("/api/v1/auth/register");
        when(rateLimiter.isAllowed(eq("auth:192.168.1.100"), eq(20), eq(60))).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(rateLimiter).isAllowed("auth:192.168.1.100", 20, 60);
        verify(filterChain).doFilter(request, response);
    }

    // ==================== API Endpoint Rate Limiting ====================

    @Test
    void doFilter_apiEndpoint_withinLimit_passesThrough() throws Exception {
        request.setRequestURI("/api/v1/snippets");
        when(rateLimiter.isAllowed(eq("api:192.168.1.100"), eq(300), eq(60))).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_apiEndpoint_exceedsLimit_returns429() throws Exception {
        request.setRequestURI("/api/v1/snippets");
        when(rateLimiter.isAllowed(eq("api:192.168.1.100"), eq(300), eq(60))).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(429);
    }

    // ==================== Non-API Endpoints ====================

    @Test
    void doFilter_nonApiEndpoint_passesWithoutRateLimiting() throws Exception {
        request.setRequestURI("/login");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(rateLimiter);
    }

    @Test
    void doFilter_staticResourceEndpoint_passesWithoutRateLimiting() throws Exception {
        request.setRequestURI("/css/style.css");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(rateLimiter);
    }

    // ==================== Response Format ====================

    @Test
    void rateLimitResponse_containsExpectedJsonFields() throws Exception {
        request.setRequestURI("/api/v1/auth/login");
        when(rateLimiter.isAllowed(anyString(), anyInt(), anyInt())).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        String body = response.getContentAsString();
        assertThat(body).contains("\"code\":\"RATE_LIMIT_EXCEEDED\"");
        assertThat(body).contains("\"message\":\"Too many requests. Please try again later.\"");
        assertThat(body).contains("\"path\":\"/api/v1/auth/login\"");
        assertThat(body).contains("\"timestamp\":");
    }

    // ==================== Client Identifier ====================

    @Test
    void doFilter_usesRemoteAddr_notForwardedHeader() throws Exception {
        request.setRequestURI("/api/v1/snippets");
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "evil-spoofed-ip");
        when(rateLimiter.isAllowed(eq("api:10.0.0.1"), eq(300), eq(60))).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        // Should use remoteAddr (10.0.0.1), not the X-Forwarded-For header
        verify(rateLimiter).isAllowed("api:10.0.0.1", 300, 60);
        verify(rateLimiter, never()).isAllowed(contains("evil"), anyInt(), anyInt());
    }
}

