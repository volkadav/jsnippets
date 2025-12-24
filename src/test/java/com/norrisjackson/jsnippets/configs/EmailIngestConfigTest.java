package com.norrisjackson.jsnippets.configs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailIngestConfigTest {

    private EmailIngestConfig config;

    @BeforeEach
    void setUp() {
        config = new EmailIngestConfig();
    }

    @Test
    void isConfigured_WithAllRequiredFields_ReturnsTrue() {
        config.setEnabled(true);
        config.setHost("imap.example.com");
        config.setUsername("user@example.com");
        config.setPassword("password");

        assertTrue(config.isConfigured());
    }

    @Test
    void isConfigured_WhenDisabled_ReturnsFalse() {
        config.setEnabled(false);
        config.setHost("imap.example.com");
        config.setUsername("user@example.com");
        config.setPassword("password");

        assertFalse(config.isConfigured());
    }

    @Test
    void isConfigured_WithMissingHost_ReturnsFalse() {
        config.setEnabled(true);
        config.setHost(null);
        config.setUsername("user@example.com");
        config.setPassword("password");

        assertFalse(config.isConfigured());
    }

    @Test
    void isConfigured_WithEmptyHost_ReturnsFalse() {
        config.setEnabled(true);
        config.setHost("");
        config.setUsername("user@example.com");
        config.setPassword("password");

        assertFalse(config.isConfigured());
    }

    @Test
    void isConfigured_WithMissingUsername_ReturnsFalse() {
        config.setEnabled(true);
        config.setHost("imap.example.com");
        config.setUsername(null);
        config.setPassword("password");

        assertFalse(config.isConfigured());
    }

    @Test
    void isConfigured_WithMissingPassword_ReturnsFalse() {
        config.setEnabled(true);
        config.setHost("imap.example.com");
        config.setUsername("user@example.com");
        config.setPassword(null);

        assertFalse(config.isConfigured());
    }

    @Test
    void defaultValues_AreCorrect() {
        assertFalse(config.isEnabled());
        assertEquals("imap", config.getProtocol());
        assertEquals(993, config.getPort());
        assertTrue(config.isSslEnabled());
        assertFalse(config.isStarttlsEnabled());
        assertEquals(10, config.getPollIntervalMinutes());
        assertEquals(1, config.getInitialDelayMinutes());
        assertEquals("INBOX", config.getFolder());
        assertEquals(30, config.getRetentionDays());
    }
}

