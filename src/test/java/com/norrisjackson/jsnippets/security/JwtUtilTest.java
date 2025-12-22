package com.norrisjackson.jsnippets.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtUtil class.
 * Tests token generation, validation, and extraction.
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private UserDetails userDetails;
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_SECRET = "test-secret-key-must-be-at-least-256-bits-long-for-hs256";
    private static final Long TEST_EXPIRATION = 3600000L; // 1 hour

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();

        // Set test values using reflection
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", TEST_EXPIRATION);

        // Create test user details
        userDetails = User.builder()
                .username(TEST_USERNAME)
                .password("password")
                .authorities(new ArrayList<>())
                .build();
    }

    @Test
    void generateToken_WithValidUserDetails_ReturnsToken() {
        String token = jwtUtil.generateToken(userDetails);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertEquals(3, token.split("\\.").length); // JWT has 3 parts
    }

    @Test
    void extractUsername_FromValidToken_ReturnsUsername() {
        String token = jwtUtil.generateToken(userDetails);

        String extractedUsername = jwtUtil.extractUsername(token);

        assertEquals(TEST_USERNAME, extractedUsername);
    }

    @Test
    void extractExpiration_FromValidToken_ReturnsExpirationDate() {
        String token = jwtUtil.generateToken(userDetails);

        Date expiration = jwtUtil.extractExpiration(token);

        assertNotNull(expiration);
        assertTrue(expiration.after(new Date())); // Should be in the future
    }

    @Test
    void validateToken_WithValidToken_ReturnsTrue() {
        String token = jwtUtil.generateToken(userDetails);

        Boolean isValid = jwtUtil.validateToken(token, userDetails);

        assertTrue(isValid);
    }

    @Test
    void validateToken_WithWrongUsername_ReturnsFalse() {
        String token = jwtUtil.generateToken(userDetails);

        UserDetails wrongUser = User.builder()
                .username("wronguser")
                .password("password")
                .authorities(new ArrayList<>())
                .build();

        Boolean isValid = jwtUtil.validateToken(token, wrongUser);

        assertFalse(isValid);
    }

    @Test
    void validateToken_WithExpiredToken_ReturnsFalse() {
        // Create a token with very short expiration
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1000L); // Already expired
        String token = jwtUtil.generateToken(userDetails);

        // Reset to normal expiration for validation
        ReflectionTestUtils.setField(jwtUtil, "expiration", TEST_EXPIRATION);

        Boolean isValid = jwtUtil.validateToken(token, userDetails);

        assertFalse(isValid);
    }

    @Test
    void validateToken_WithInvalidToken_ReturnsFalse() {
        String invalidToken = "invalid.token.here";

        Boolean isValid = jwtUtil.validateToken(invalidToken, userDetails);

        assertFalse(isValid);
    }

    @Test
    void validateToken_WithoutUserDetails_ValidatesBasicToken() {
        String token = jwtUtil.generateToken(userDetails);

        Boolean isValid = jwtUtil.validateToken(token);

        assertTrue(isValid);
    }

    @Test
    void validateToken_WithoutUserDetails_InvalidToken_ReturnsFalse() {
        String invalidToken = "invalid.token.here";

        Boolean isValid = jwtUtil.validateToken(invalidToken);

        assertFalse(isValid);
    }

    @Test
    void generateToken_CreatesTokenWithCorrectExpiration() {
        String token = jwtUtil.generateToken(userDetails);
        Date expiration = jwtUtil.extractExpiration(token);
        Date issuedAt = new Date();

        // Check expiration is approximately 1 hour from now (with 5 second tolerance)
        long expectedExpiration = issuedAt.getTime() + TEST_EXPIRATION;
        long actualExpiration = expiration.getTime();

        assertTrue(Math.abs(expectedExpiration - actualExpiration) < 5000);
    }

    @Test
    void generateToken_TwoDifferentTokensForSameUser_AreDifferent() {
        String token1 = jwtUtil.generateToken(userDetails);

        // Wait a bit to ensure different timestamp (1 second to guarantee different iat claim)
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // Ignore
        }

        String token2 = jwtUtil.generateToken(userDetails);

        assertNotEquals(token1, token2); // Different due to different issuedAt times
    }
}

