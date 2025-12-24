package com.norrisjackson.jsnippets.services.email;

import com.norrisjackson.jsnippets.configs.EmailIngestConfig;
import com.norrisjackson.jsnippets.data.ProcessedEmailRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Service for cleaning up old processed email records based on retention policy.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "email.ingest.enabled", havingValue = "true")
public class ProcessedEmailCleanupService {

    private final ProcessedEmailRepository repository;
    private final EmailIngestConfig config;

    public ProcessedEmailCleanupService(ProcessedEmailRepository repository, EmailIngestConfig config) {
        this.repository = repository;
        this.config = config;
    }

    /**
     * Clean up old processed email records based on retention policy.
     * Runs daily at 2 AM.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOldRecords() {
        int retentionDays = config.getRetentionDays();
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);

        log.info("Starting cleanup of processed email records older than {} days", retentionDays);

        try {
            int deleted = repository.deleteByProcessedAtBefore(cutoff);
            log.info("Cleaned up {} processed email record(s) older than {} days", deleted, retentionDays);
        } catch (Exception e) {
            log.error("Error during processed email cleanup: {}", e.getMessage(), e);
        }
    }
}

