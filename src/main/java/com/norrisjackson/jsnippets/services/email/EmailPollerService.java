package com.norrisjackson.jsnippets.services.email;

import com.norrisjackson.jsnippets.configs.EmailIngestConfig;
import jakarta.mail.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Scheduled service that polls an email account for new messages
 * and processes them into snippets.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "email.ingest.enabled", havingValue = "true")
public class EmailPollerService {

    private final EmailIngestConfig config;
    private final EmailProcessorService emailProcessor;

    public EmailPollerService(EmailIngestConfig config, EmailProcessorService emailProcessor) {
        this.config = config;
        this.emailProcessor = emailProcessor;
        log.info("Email polling service initialized - will poll every {} minutes", config.getPollIntervalMinutes());
    }

    /**
     * Poll the configured email account for new messages.
     * Runs on a fixed delay schedule configured via properties.
     */
    @Scheduled(fixedDelayString = "${email.ingest.poll-interval-minutes:10}",
            timeUnit = TimeUnit.MINUTES,
            initialDelayString = "${email.ingest.initial-delay-minutes:1}")
    public void pollForEmails() {
        if (!config.isConfigured()) {
            log.warn("Email ingestion enabled but not properly configured - skipping poll");
            return;
        }

        log.info("Starting email poll from {}:{}", config.getHost(), config.getPort());

        Store store = null;
        Folder inbox = null;

        try {
            store = connectToMailServer();
            inbox = store.getFolder(config.getFolder());
            inbox.open(Folder.READ_WRITE);

            Message[] messages = inbox.getMessages();
            log.info("Found {} message(s) in {}", messages.length, config.getFolder());

            int processed = 0;
            int errors = 0;

            for (Message message : messages) {
                try {
                    boolean shouldDelete = emailProcessor.processMessage(message);
                    if (shouldDelete) {
                        message.setFlag(Flags.Flag.DELETED, true);
                    }
                    processed++;
                } catch (Exception e) {
                    log.error("Unexpected error processing message: {}", e.getMessage());
                    errors++;
                }
            }

            log.info("Email poll complete - processed: {}, errors: {}", processed, errors);

        } catch (AuthenticationFailedException e) {
            log.error("Email authentication failed: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Error polling email: {}", e.getMessage(), e);
        } finally {
            closeQuietly(inbox, true);
            closeQuietly(store);
        }
    }

    /**
     * Connect to the mail server.
     */
    private Store connectToMailServer() throws MessagingException {
        Properties props = config.buildMailProperties(10_000, 10_000);
        Session session = Session.getInstance(props);
        Store store = session.getStore(config.getProtocol());
        store.connect(
            config.getHost(),
            config.getPort(),
            config.getUsername(),
            config.getPassword()
        );
        return store;
    }


    /**
     * Close a folder quietly, optionally expunging deleted messages.
     */
    private void closeQuietly(Folder folder, boolean expunge) {
        if (folder != null && folder.isOpen()) {
            try {
                folder.close(expunge);
            } catch (Exception e) {
                log.debug("Error closing folder: {}", e.getMessage());
            }
        }
    }

    /**
     * Close a store quietly.
     */
    private void closeQuietly(Store store) {
        if (store != null && store.isConnected()) {
            try {
                store.close();
            } catch (Exception e) {
                log.debug("Error closing store: {}", e.getMessage());
            }
        }
    }
}
