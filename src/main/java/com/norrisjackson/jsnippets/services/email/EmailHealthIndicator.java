package com.norrisjackson.jsnippets.services.email;

import com.norrisjackson.jsnippets.configs.EmailIngestConfig;
import jakarta.mail.Session;
import jakarta.mail.Store;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * Health indicator for the email ingestion feature.
 * Checks connectivity to the configured mail server.
 */
@Component("emailIngestHealth")
@Slf4j
@ConditionalOnProperty(name = "email.ingest.enabled", havingValue = "true")
public class EmailHealthIndicator implements HealthIndicator {

    private final EmailIngestConfig config;

    public EmailHealthIndicator(EmailIngestConfig config) {
        this.config = config;
    }

    @Override
    public Health health() {
        if (!config.isConfigured()) {
            return Health.unknown()
                    .withDetail("reason", "Email ingestion not fully configured")
                    .withDetail("enabled", config.isEnabled())
                    .withDetail("hostConfigured", config.getHost() != null)
                    .build();
        }

        try {
            // Attempt to connect to mail server
            Properties props = buildMailProperties();
            Session session = Session.getInstance(props);
            Store store = session.getStore(config.getProtocol());

            long startTime = System.currentTimeMillis();
            store.connect(
                    config.getHost(),
                    config.getPort(),
                    config.getUsername(),
                    config.getPassword()
            );
            long connectionTime = System.currentTimeMillis() - startTime;
            store.close();

            return Health.up()
                    .withDetail("host", config.getHost())
                    .withDetail("port", config.getPort())
                    .withDetail("protocol", config.getProtocol())
                    .withDetail("folder", config.getFolder())
                    .withDetail("connectionTimeMs", connectionTime)
                    .build();

        } catch (Exception e) {
            log.warn("Email health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("host", config.getHost())
                    .withDetail("port", config.getPort())
                    .withDetail("protocol", config.getProtocol())
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }

    /**
     * Build mail session properties for health check.
     */
    private Properties buildMailProperties() {
        Properties props = new Properties();
        String protocol = config.getProtocol();

        props.put("mail." + protocol + ".host", config.getHost());
        props.put("mail." + protocol + ".port", String.valueOf(config.getPort()));

        if (config.isSslEnabled()) {
            props.put("mail." + protocol + ".ssl.enable", "true");
            props.put("mail." + protocol + ".socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail." + protocol + ".socketFactory.fallback", "false");
        }

        if (config.isStarttlsEnabled()) {
            props.put("mail." + protocol + ".starttls.enable", "true");
        }

        // Shorter timeout for health checks
        props.put("mail." + protocol + ".connectiontimeout", "5000");
        props.put("mail." + protocol + ".timeout", "5000");

        return props;
    }
}

