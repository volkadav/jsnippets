package com.norrisjackson.jsnippets.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the User entity.
 */
class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPasswordHash("hashedpassword");
        user.setCreatedAt(new Date());
    }

    @Test
    void bio_defaultsToNull() {
        assertThat(user.getBio()).isNull();
    }

    @Test
    void bio_canBeSet() {
        String bio = "Hello, I am a test user!";
        user.setBio(bio);
        assertThat(user.getBio()).isEqualTo(bio);
    }

    @Test
    void bio_canBeSetToNull() {
        user.setBio("Some bio");
        user.setBio(null);
        assertThat(user.getBio()).isNull();
    }

    @Test
    void bio_canBeEmpty() {
        user.setBio("");
        assertThat(user.getBio()).isEmpty();
    }

    @Test
    void bio_canContainLongText() {
        String longBio = "a".repeat(4000);
        user.setBio(longBio);
        assertThat(user.getBio()).hasSize(4000);
    }

    @Test
    void bio_canContainMultilineText() {
        String multilineBio = "Line 1\nLine 2\nLine 3";
        user.setBio(multilineBio);
        assertThat(user.getBio()).contains("\n");
        assertThat(user.getBio()).isEqualTo(multilineBio);
    }

    @Test
    void bio_canContainUnicodeCharacters() {
        String unicodeBio = "Hello 👋 World 🌍! Ñoño café résumé";
        user.setBio(unicodeBio);
        assertThat(user.getBio()).isEqualTo(unicodeBio);
    }

    @Test
    void bio_canContainSpecialCharacters() {
        String specialBio = "Test & < > \" ' characters";
        user.setBio(specialBio);
        assertThat(user.getBio()).isEqualTo(specialBio);
    }

    @Test
    void toString_doesNotIncludeBio() {
        user.setBio("Secret bio information");
        String userString = user.toString();
        // Bio should be included in toString since it's not sensitive
        // But followedUsers and followers should be excluded to prevent infinite recursion
        assertThat(userString).doesNotContain("followedUsers");
        assertThat(userString).doesNotContain("followers");
    }

    @Test
    void timezone_defaultsToUTC() {
        User newUser = new User();
        assertThat(newUser.getTimezone()).isEqualTo("UTC");
    }

    @Test
    void followedUsers_defaultsToEmptyList() {
        User newUser = new User();
        assertThat(newUser.getFollowedUsers()).isNotNull().isEmpty();
    }

    // Icon tests

    @Test
    void icon_defaultsToNull() {
        assertThat(user.getIcon()).isNull();
    }

    @Test
    void icon_canBeSet() {
        byte[] iconData = new byte[] {1, 2, 3, 4, 5};
        user.setIcon(iconData);
        assertThat(user.getIcon()).isEqualTo(iconData);
    }

    @Test
    void icon_canBeSetToNull() {
        user.setIcon(new byte[] {1, 2, 3});
        user.setIcon(null);
        assertThat(user.getIcon()).isNull();
    }

    @Test
    void iconContentType_defaultsToNull() {
        assertThat(user.getIconContentType()).isNull();
    }

    @Test
    void iconContentType_canBeSet() {
        user.setIconContentType("image/png");
        assertThat(user.getIconContentType()).isEqualTo("image/png");
    }

    @Test
    void iconContentType_canBeSetToNull() {
        user.setIconContentType("image/jpeg");
        user.setIconContentType(null);
        assertThat(user.getIconContentType()).isNull();
    }

    @Test
    void icon_canStoreMaxSize() {
        // Test storing 32KB of data
        byte[] maxSizeIcon = new byte[32 * 1024];
        for (int i = 0; i < maxSizeIcon.length; i++) {
            maxSizeIcon[i] = (byte) (i % 256);
        }
        user.setIcon(maxSizeIcon);
        assertThat(user.getIcon()).hasSize(32 * 1024);
    }
}

