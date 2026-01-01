package com.norrisjackson.jsnippets.services.email;

import com.norrisjackson.jsnippets.configs.EmailIngestConfig;
import com.norrisjackson.jsnippets.data.*;
import com.norrisjackson.jsnippets.services.SnippetService;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date; // Used for jakarta.mail Message API
import java.util.Optional;

/**
 * Service for processing individual email messages and creating snippets.
 */
@Service
@Slf4j
public class EmailProcessorService {

    private final UserRepository userRepository;
    private final SnippetService snippetService;
    private final ProcessedEmailRepository processedEmailRepository;
    private final EmailIngestConfig config;

    public EmailProcessorService(UserRepository userRepository,
                                 SnippetService snippetService,
                                 ProcessedEmailRepository processedEmailRepository,
                                 EmailIngestConfig config) {
        this.userRepository = userRepository;
        this.snippetService = snippetService;
        this.processedEmailRepository = processedEmailRepository;
        this.config = config;
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
     */
    private String extractTextBody(Message message) throws Exception {
        try {
            // Check if message has any content
            Object content = message.getContent();
            if (content == null) {
                return null;
            }

            if (message.isMimeType("text/plain")) {
                return (String) content;
            }

            if (message.isMimeType("multipart/*")) {
                return extractTextFromMultipart((Multipart) content);
            }

            // For HTML-only emails, strip tags (basic approach for v1)
            if (message.isMimeType("text/html")) {
                return stripHtmlTags((String) content);
            }

            return null;
        } catch (Exception e) {
            // Message has no content or content cannot be read
            log.debug("Could not extract body from message: {}", e.getMessage());
            return null;
        }
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
     * Strip HTML tags from content.
     * Basic implementation for v1 - consider using Jsoup for production.
     */
    private String stripHtmlTags(String html) {
        if (html == null) return null;
        return html.replaceAll("<[^>]*>", "")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&quot;", "\"")
                .replaceAll("&#39;", "'")
                .replaceAll("\\s+", " ")
                .trim();
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
            processedEmailRepository.save(record);
        } catch (Exception e) {
            log.error("Failed to record processed email: {}", e.getMessage());
        }
    }
}

