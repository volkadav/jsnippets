package com.norrisjackson.jsnippets.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Entity for tracking processed email messages to prevent duplicate processing.
 */
@Entity
@Table(name = "processed_emails")
@Getter
@Setter
@NoArgsConstructor
public class ProcessedEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false, unique = true)
    private String messageId;

    @Column(name = "sender_email")
    private String senderEmail;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @Column(name = "snippet_id")
    private Long snippetId;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ProcessingStatus status;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "original_subject")
    private String originalSubject;

    @Column(name = "original_body")
    private String originalBody;

    public ProcessedEmail(String messageId, String senderEmail, ProcessingStatus status) {
        this.messageId = messageId;
        this.senderEmail = senderEmail;
        this.status = status;
        this.processedAt = Instant.now();
    }
}

