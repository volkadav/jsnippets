package com.norrisjackson.jsnippets.services.email;

import com.norrisjackson.jsnippets.configs.EmailIngestConfig;
import com.norrisjackson.jsnippets.data.ProcessedEmailRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessedEmailCleanupServiceTest {

    @Mock
    private ProcessedEmailRepository repository;

    @Mock
    private EmailIngestConfig config;

    private ProcessedEmailCleanupService cleanupService;

    @BeforeEach
    void setUp() {
        cleanupService = new ProcessedEmailCleanupService(repository, config);
    }

    @Test
    void cleanupOldRecords_DeletesRecordsOlderThanRetentionDays() {
        // Arrange
        when(config.getRetentionDays()).thenReturn(30);
        when(repository.deleteByProcessedAtBefore(any(Instant.class))).thenReturn(5);

        // Act
        cleanupService.cleanupOldRecords();

        // Assert
        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        verify(repository).deleteByProcessedAtBefore(captor.capture());

        Instant cutoff = captor.getValue();
        Instant expectedCutoff = Instant.now().minus(30, ChronoUnit.DAYS);

        // Allow 1 second tolerance
        assertTrue(Math.abs(cutoff.toEpochMilli() - expectedCutoff.toEpochMilli()) < 1000);
    }

    @Test
    void cleanupOldRecords_UsesConfiguredRetentionDays() {
        // Arrange
        when(config.getRetentionDays()).thenReturn(7);
        when(repository.deleteByProcessedAtBefore(any(Instant.class))).thenReturn(10);

        // Act
        cleanupService.cleanupOldRecords();

        // Assert
        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        verify(repository).deleteByProcessedAtBefore(captor.capture());

        Instant cutoff = captor.getValue();
        Instant expectedCutoff = Instant.now().minus(7, ChronoUnit.DAYS);

        // Allow 1 second tolerance
        assertTrue(Math.abs(cutoff.toEpochMilli() - expectedCutoff.toEpochMilli()) < 1000);
    }

    @Test
    void cleanupOldRecords_HandlesException() {
        // Arrange
        when(config.getRetentionDays()).thenReturn(30);
        when(repository.deleteByProcessedAtBefore(any(Instant.class)))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> cleanupService.cleanupOldRecords());
    }
}

