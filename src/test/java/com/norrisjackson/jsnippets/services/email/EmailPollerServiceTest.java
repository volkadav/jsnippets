package com.norrisjackson.jsnippets.services.email;

import com.norrisjackson.jsnippets.configs.EmailIngestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
class EmailPollerServiceTest {

    private EmailIngestConfig realConfig;

    @BeforeEach
    void setUp() {
        realConfig = new EmailIngestConfig();
    }

    @Test
    void buildMailProperties_WithImapSsl_ConfiguresCorrectly() {
        // Arrange
        realConfig.setProtocol("imap");
        realConfig.setHost("imap.example.com");
        realConfig.setPort(993);
        realConfig.setSslEnabled(true);
        realConfig.setStarttlsEnabled(false);

        // Act
        Properties props = realConfig.buildMailProperties(10_000, 10_000);

        // Assert
        assertEquals("imap.example.com", props.getProperty("mail.imap.host"));
        assertEquals("993", props.getProperty("mail.imap.port"));
        assertEquals("true", props.getProperty("mail.imap.ssl.enable"));
    }

    @Test
    void buildMailProperties_WithPop3Starttls_ConfiguresCorrectly() {
        // Arrange
        realConfig.setProtocol("pop3");
        realConfig.setHost("pop.example.com");
        realConfig.setPort(995);
        realConfig.setSslEnabled(false);
        realConfig.setStarttlsEnabled(true);

        // Act
        Properties props = realConfig.buildMailProperties(10_000, 10_000);

        // Assert
        assertEquals("pop.example.com", props.getProperty("mail.pop3.host"));
        assertEquals("995", props.getProperty("mail.pop3.port"));
        assertEquals("true", props.getProperty("mail.pop3.starttls.enable"));
        assertNull(props.getProperty("mail.pop3.ssl.enable"));
    }

    @Test
    void buildMailProperties_IncludesTimeoutSettings() {
        // Arrange
        realConfig.setProtocol("imap");
        realConfig.setHost("imap.example.com");
        realConfig.setPort(993);
        realConfig.setSslEnabled(true);
        realConfig.setStarttlsEnabled(false);

        // Act
        Properties props = realConfig.buildMailProperties(10_000, 10_000);

        // Assert
        assertEquals("10000", props.getProperty("mail.imap.connectiontimeout"));
        assertEquals("10000", props.getProperty("mail.imap.timeout"));
    }
}

