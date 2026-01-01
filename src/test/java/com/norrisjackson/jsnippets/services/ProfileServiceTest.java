package com.norrisjackson.jsnippets.services;

import com.norrisjackson.jsnippets.data.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for profile-related functionality
 * Tests that the User entity works properly with timezone field
 */
class ProfileServiceTest {

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setCreatedAt(Instant.now());
        testUser.setTimezone("America/New_York");
    }

    @Test
    void user_WhenCreatedWithTimezone_HasCorrectTimezone() {
        // Then
        assertThat(testUser.getTimezone()).isEqualTo("America/New_York");
    }

    @Test
    void user_WhenTimezoneUpdated_ReturnsNewTimezone() {
        // When
        testUser.setTimezone("Europe/London");

        // Then
        assertThat(testUser.getTimezone()).isEqualTo("Europe/London");
    }

    @Test
    void user_WhenCreatedWithDefaultConstructor_HasUTCTimezone() {
        // Given
        User newUser = new User();

        // Then
        assertThat(newUser.getTimezone()).isEqualTo("UTC");
    }
}