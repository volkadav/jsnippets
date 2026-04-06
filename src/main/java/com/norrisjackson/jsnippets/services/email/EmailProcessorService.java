package com.norrisjackson.jsnippets.services.email;

import com.norrisjackson.jsnippets.configs.EmailIngestConfig;
import com.norrisjackson.jsnippets.data.*;
import com.norrisjackson.jsnippets.services.SnippetService;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Date; // Used for jakarta.mail Message API
import java.util.Optional;

/**
 * Service for processing individual email messages and creating snippets.
 */
@Service
@Slf4j
public class EmailProcessorService {

    /** Maximum text body length to process (100 KB) to prevent excessive memory usage. */
    private static final int MAX_BODY_LENGTH = 100_000;

    private final UserRepository userRepository;
    private final SnippetService snippetService;
    private final ProcessedEmailRepository processedEmailRepository;
    private final TransactionTemplate requiresNewTx;

    public EmailProcessorService(UserRepository userRepository,
                                 SnippetService snippetService,
                                 ProcessedEmailRepository processedEmailRepository,
                                 PlatformTransactionManager txManager) {
        this.userRepository = userRepository;
        this.snippetService = snippetService;
        this.processedEmailRepository = processedEmailRepository;
        this.requiresNewTx = new TransactionTemplate(txManager);
        this.requiresNewTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Process a single email message and create a snippet if valid.
     *
     * @param message the email message to process
     * @return true if the message should be deleted from the server
     */
    @Transactional
    public boolean processMessage(Message message) {
        String messageId = null;
        String senderEmail = null;
        String originalSubject = null;
        String originalBody = null;

        try {
            messageId = getMessageId(message);

            // Check if already processed
            if (messageId != null && processedEmailRepository.existsByMessageId(messageId)) {
                log.debug("Skipping already processed message: {}", messageId);
                return true; // Delete duplicate
            }

            // Extract original content early for storage
            originalSubject = message.getSubject();
            originalBody = extractTextBody(message);

            senderEmail = extractSenderEmail(message);
            if (senderEmail == null) {
                log.warn("Could not extract sender email from message: {}", messageId);
                recordProcessedEmail(messageId, null, null, ProcessingStatus.PROCESSING_ERROR,
                        "Could not extract sender email", originalSubject, originalBody);
                return true; // Delete malformed message
            }

            // Look up user by email
            Optional<User> userOpt = userRepository.findByEmail(senderEmail);
            if (userOpt.isEmpty()) {
                log.warn("No user found for sender email: {}", senderEmail);
                recordProcessedEmail(messageId, senderEmail, null, ProcessingStatus.USER_NOT_FOUND,
                        null, originalSubject, originalBody);
                return true; // Delete message from unknown sender
            }

            User user = userOpt.get();

            // Extract snippet content from subject and body
            String content = combineSubjectAndBody(originalSubject, originalBody);
            if (content == null || content.isBlank()) {
                log.warn("Empty content in email from: {}", senderEmail);
                recordProcessedEmail(messageId, senderEmail, null, ProcessingStatus.EMPTY_CONTENT,
                        null, originalSubject, originalBody);
                return true; // Delete empty message
            }

            // Get message received date for snippet timestamp
            Date receivedDate = message.getReceivedDate();
            if (receivedDate == null) {
                receivedDate = message.getSentDate();
            }
            // Convert to Instant, defaulting to now if no date available
            Instant receivedInstant = receivedDate != null ? receivedDate.toInstant() : Instant.now();

            // Create snippet
            Snippet snippet = snippetService.createSnippetWithDate(content, user, receivedInstant);

            log.info("Created snippet {} from email by user {} ({})",
                    snippet.getId(), user.getUsername(), senderEmail);

            recordProcessedEmail(messageId, senderEmail, snippet.getId(), ProcessingStatus.SUCCESS,
                    null, originalSubject, originalBody);
            return true; // Delete successfully processed message

        } catch (Exception e) {
            log.error("Error processing email from {}: {}", senderEmail, e.getMessage(), e);
            recordProcessedEmail(messageId, senderEmail, null, ProcessingStatus.PROCESSING_ERROR,
                    e.getMessage(), originalSubject, originalBody);
            return false; // Do NOT delete - leave for retry on next poll
        }
    }

    /**
     * Combine subject and body into snippet content.
     * If both exist, concatenate with space separator.
     * If only one exists, use that.
     */
    private String combineSubjectAndBody(String subject, String body) {
        subject = (subject != null) ? subject.trim() : "";
        body = (body != null) ? body.trim() : "";

        if (!subject.isEmpty() && !body.isEmpty()) {
            return subject + " " + body;
        } else if (!subject.isEmpty()) {
            return subject;
        } else if (!body.isEmpty()) {
            return body;
        }

        return null;
    }

    /**
     * Extract plain text body from message, handling multipart messages.
     * Truncates to MAX_BODY_LENGTH to prevent excessive memory usage.
     */
    private String extractTextBody(Message message) {
        try {
            // Check if message has any content
            Object content = message.getContent();
            if (content == null) {
                return null;
            }

            String body = null;
            if (message.isMimeType("text/plain")) {
                body = (String) content;
            } else if (message.isMimeType("multipart/*")) {
                body = extractTextFromMultipart((Multipart) content);
            } else if (message.isMimeType("text/html")) {
                body = stripHtmlTags((String) content);
            }

            return truncateIfNeeded(body);
        } catch (Exception e) {
            // Message has no content or content cannot be read
            log.debug("Could not extract body from message: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Truncate text to MAX_BODY_LENGTH if it exceeds the limit.
     */
    private String truncateIfNeeded(String text) {
        if (text != null && text.length() > MAX_BODY_LENGTH) {
            log.warn("Email body truncated from {} to {} characters", text.length(), MAX_BODY_LENGTH);
            return text.substring(0, MAX_BODY_LENGTH);
        }
        return text;
    }

    private String extractTextFromMultipart(Multipart multipart) throws Exception {
        // First pass: look for plain text
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);

            // Skip attachments
            if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                continue;
            }

            if (part.isMimeType("text/plain")) {
                return (String) part.getContent();
            }

            if (part.isMimeType("multipart/*")) {
                String text = extractTextFromMultipart((Multipart) part.getContent());
                if (text != null) {
                    return text;
                }
            }
        }

        // Second pass: fallback to HTML if no plain text found
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            if (part.isMimeType("text/html")) {
                return stripHtmlTags((String) part.getContent());
            }
        }

        return null;
    }

    /**
     * Extract plain text from HTML content using Jsoup.
     * Handles all HTML entities, nested tags, and malformed HTML safely.
     */
    private String stripHtmlTags(String html) {
        if (html == null) return null;
        return Jsoup.parse(html).text();
    }

    /**
     * Extract sender email address from message.
     */
    private String extractSenderEmail(Message message) throws Exception {
        Address[] from = message.getFrom();
        if (from != null && from.length > 0) {
            if (from[0] instanceof InternetAddress) {
                return ((InternetAddress) from[0]).getAddress().toLowerCase();
            }
            return from[0].toString().toLowerCase();
        }
        return null;
    }

    /**
     * Get the Message-ID header value.
     */
    private String getMessageId(Message message) {
        try {
            String[] headers = message.getHeader("Message-ID");
            if (headers != null && headers.length > 0) {
                return headers[0];
            }
        } catch (Exception e) {
            log.debug("Could not extract Message-ID: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Record a processed email in the database.
     * Uses a REQUIRES_NEW transaction so the record is persisted even if
     * the outer transaction (from processMessage) is rolled back.
     */
    private void recordProcessedEmail(String messageId, String senderEmail,
                                      Long snippetId, ProcessingStatus status, String errorMessage,
                                      String originalSubject, String originalBody) {
        if (messageId == null) {
            messageId = "unknown-" + System.currentTimeMillis();
        }

        ProcessedEmail record = new ProcessedEmail();
        record.setMessageId(messageId);
        record.setSenderEmail(senderEmail);
        record.setSnippetId(snippetId);
        record.setProcessedAt(Instant.now());
        record.setStatus(status);
        record.setErrorMessage(errorMessage);
        record.setOriginalSubject(originalSubject);
        record.setOriginalBody(originalBody);

        try {
            requiresNewTx.executeWithoutResult(txStatus ->
                    processedEmailRepository.save(record));
        } catch (Exception e) {
            log.error("Failed to record processed email: {}", e.getMessage());
        }
    }
}

