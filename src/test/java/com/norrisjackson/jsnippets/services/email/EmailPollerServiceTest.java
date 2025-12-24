package com.norrisjackson.jsnippets.services.email;

import com.norrisjackson.jsnippets.configs.EmailIngestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailPollerServiceTest {

    @Mock
    private EmailIngestConfig config;

    @Mock
    private EmailProcessorService emailProcessor;

    private EmailPollerService emailPollerService;

    @BeforeEach
    void setUp() {
        when(config.getPollIntervalMinutes()).thenReturn(10);
        emailPollerService = new EmailPollerService(config, emailProcessor);
    }

    @Test
    void buildMailProperties_WithImapSsl_ConfiguresCorrectly() {
        // Arrange
        when(config.getProtocol()).thenReturn("imap");
        when(config.getHost()).thenReturn("imap.example.com");
        when(config.getPort()).thenReturn(993);
        when(config.isSslEnabled()).thenReturn(true);
        when(config.isStarttlsEnabled()).thenReturn(false);

        // Act
        Properties props = emailPollerService.buildMailProperties();

        // Assert
        assertEquals("imap.example.com", props.getProperty("mail.imap.host"));
        assertEquals("993", props.getProperty("mail.imap.port"));
        assertEquals("true", props.getProperty("mail.imap.ssl.enable"));
    }

    @Test
    void buildMailProperties_WithPop3Starttls_ConfiguresCorrectly() {
        // Arrange
        when(config.getProtocol()).thenReturn("pop3");
        when(config.getHost()).thenReturn("pop.example.com");
        when(config.getPort()).thenReturn(995);
        when(config.isSslEnabled()).thenReturn(false);
        when(config.isStarttlsEnabled()).thenReturn(true);

        // Act
        Properties props = emailPollerService.buildMailProperties();

        // Assert
        assertEquals("pop.example.com", props.getProperty("mail.pop3.host"));
        assertEquals("995", props.getProperty("mail.pop3.port"));
        assertEquals("true", props.getProperty("mail.pop3.starttls.enable"));
        assertNull(props.getProperty("mail.pop3.ssl.enable"));
    }

    @Test
    void buildMailProperties_IncludesTimeoutSettings() {
        // Arrange
        when(config.getProtocol()).thenReturn("imap");
        when(config.getHost()).thenReturn("imap.example.com");
        when(config.getPort()).thenReturn(993);
        when(config.isSslEnabled()).thenReturn(true);
        when(config.isStarttlsEnabled()).thenReturn(false);

        // Act
        Properties props = emailPollerService.buildMailProperties();

        // Assert
        assertEquals("10000", props.getProperty("mail.imap.connectiontimeout"));
        assertEquals("10000", props.getProperty("mail.imap.timeout"));
    }
}

