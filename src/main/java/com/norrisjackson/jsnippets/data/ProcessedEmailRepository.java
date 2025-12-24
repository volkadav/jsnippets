package com.norrisjackson.jsnippets.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

/**
 * Repository for managing ProcessedEmail entities.
 */
public interface ProcessedEmailRepository extends JpaRepository<ProcessedEmail, Long> {

    /**
     * Check if a message has already been processed.
     *
     * @param messageId the email Message-ID header value
     * @return true if the message has been processed
     */
    boolean existsByMessageId(String messageId);

    /**
     * Delete processed email records older than the specified cutoff time.
     *
     * @param cutoff the cutoff instant
     * @return the number of records deleted
     */
    @Modifying
    @Query("DELETE FROM ProcessedEmail p WHERE p.processedAt < :cutoff")
    int deleteByProcessedAtBefore(@Param("cutoff") Instant cutoff);

    /**
     * Count processed emails by status.
     *
     * @param status the processing status
     * @return the count of records with that status
     */
    long countByStatus(ProcessingStatus status);
}

