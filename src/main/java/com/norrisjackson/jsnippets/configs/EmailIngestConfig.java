package com.norrisjackson.jsnippets.configs;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Configuration properties for the email-to-snippet ingestion feature.
 * Values are loaded from application.properties with prefix "email.ingest".
 */
@Configuration
@ConfigurationProperties(prefix = "email.ingest")
@Getter
@Setter
public class EmailIngestConfig {

    /**
     * Enable/disable the email polling feature.
     * Default: false
     */
    private boolean enabled = false;

    /**
     * Mail protocol (imap or pop3).
     * Default: imap
     */
    private String protocol = "imap";

    /**
     * Mail server hostname.
     */
    private String host;

    /**
     * Mail server port.
     * Default: 993 (IMAPS)
     */
    private int port = 993;

    /**
     * Mail account username.
     */
    private String username;

    /**
     * Mail account password.
     */
    private String password;

    /**
     * Enable SSL/TLS connection.
     * Default: true
     */
    private boolean sslEnabled = true;

    /**
     * Enable STARTTLS upgrade.
     * Default: false
     */
    private boolean starttlsEnabled = false;

    /**
     * Polling interval in minutes.
     * Default: 10
     */
    private int pollIntervalMinutes = 10;

    /**
     * Initial delay before first poll in minutes.
     * Default: 1
     */
    private int initialDelayMinutes = 1;

    /**
     * Mail folder to poll.
     * Default: INBOX
     */
    private String folder = "INBOX";

    /**
     * Number of days to retain processed email records.
     * Default: 30
     */
    private int retentionDays = 30;

    /**
     * Check if the configuration is complete and valid.
     *
     * @return true if all required settings are present
     */
    public boolean isConfigured() {
        return enabled
                && StringUtils.hasText(host)
                && StringUtils.hasText(username)
                && StringUtils.hasText(password);
    }
}

