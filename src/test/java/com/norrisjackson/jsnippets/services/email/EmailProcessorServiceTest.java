package com.norrisjackson.jsnippets.services.email;

import com.norrisjackson.jsnippets.configs.EmailIngestConfig;
import com.norrisjackson.jsnippets.data.*;
import com.norrisjackson.jsnippets.services.SnippetService;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.Instant;
import java.util.Date; // Still needed for jakarta.mail API
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailProcessorServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SnippetService snippetService;

    @Mock
    private ProcessedEmailRepository processedEmailRepository;

    @Mock
    private PlatformTransactionManager txManager;

    private EmailProcessorService emailProcessorService;

    private Session mailSession;

    @BeforeEach
    void setUp() {
        // Use lenient stubs: some tests (e.g. duplicate-skip) don't reach recordProcessedEmail
        // at all, so getTransaction/commit/rollback are not invoked in every test.
        lenient().when(txManager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(new SimpleTransactionStatus());
        lenient().doNothing().when(txManager).commit(any());
        lenient().doNothing().when(txManager).rollback(any());

        emailProcessorService = new EmailProcessorService(
                userRepository, snippetService, processedEmailRepository, txManager);
        mailSession = Session.getInstance(new Properties());
    }

    @Test
    void processMessage_WithValidEmail_CreatesSnippet() throws Exception {
        // Arrange
        String senderEmail = "user@example.com";
        String subject = "Test Subject";
        String body = "Test Body";
        String messageId = "<test123@example.com>";

        MimeMessage message = createMimeMessage(senderEmail, subject, body, messageId);

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail(senderEmail);

        Snippet snippet = new Snippet();
        snippet.setId(100L);
        snippet.setContents(subject + " " + body);

        when(processedEmailRepository.existsByMessageId(messageId)).thenReturn(false);
        when(userRepository.findByEmail(senderEmail)).thenReturn(Optional.of(user));
        when(snippetService.createSnippetWithDate(anyString(), any(User.class), any(Instant.class)))
                .thenReturn(snippet);

        // Act
        boolean shouldDelete = emailProcessorService.processMessage(message);

        // Assert
        assertTrue(shouldDelete);
        verify(snippetService).createSnippetWithDate(eq(subject + " " + body), eq(user), any(Instant.class));

        ArgumentCaptor<ProcessedEmail> captor = ArgumentCaptor.forClass(ProcessedEmail.class);
        verify(processedEmailRepository).save(captor.capture());

        ProcessedEmail saved = captor.getValue();
        assertEquals(messageId, saved.getMessageId());
        assertEquals(senderEmail, saved.getSenderEmail());
        assertEquals(ProcessingStatus.SUCCESS, saved.getStatus());
        assertEquals(100L, saved.getSnippetId());
    }

    @Test
    void processMessage_WithSubjectOnly_UsesSubjectAsContent() throws Exception {
        // Arrange
        String senderEmail = "user@example.com";
        String subject = "Only Subject";
        String messageId = "<test456@example.com>";

        MimeMessage message = createMimeMessageWithSubjectOnly(senderEmail, subject, messageId);

        User user = new User();
        user.setId(1L);
        user.setEmail(senderEmail);

        Snippet snippet = new Snippet();
        snippet.setId(101L);

        when(processedEmailRepository.existsByMessageId(messageId)).thenReturn(false);
        when(userRepository.findByEmail(senderEmail)).thenReturn(Optional.of(user));
        when(snippetService.createSnippetWithDate(anyString(), any(User.class), any(Instant.class)))
                .thenReturn(snippet);

        // Act
        boolean shouldDelete = emailProcessorService.processMessage(message);

        // Assert
        assertTrue(shouldDelete);
        verify(snippetService).createSnippetWithDate(eq(subject), any(User.class), any(Instant.class));
    }

    @Test
    void processMessage_WithBodyOnly_UsesBodyAsContent() throws Exception {
        // Arrange
        String senderEmail = "user@example.com";
        String body = "Only Body Content";
        String messageId = "<test789@example.com>";

        MimeMessage message = createMimeMessage(senderEmail, null, body, messageId);

        User user = new User();
        user.setId(1L);
        user.setEmail(senderEmail);

        Snippet snippet = new Snippet();
        snippet.setId(102L);

        when(processedEmailRepository.existsByMessageId(messageId)).thenReturn(false);
        when(userRepository.findByEmail(senderEmail)).thenReturn(Optional.of(user));
        when(snippetService.createSnippetWithDate(anyString(), any(User.class), any(Instant.class)))
                .thenReturn(snippet);

        // Act
        boolean shouldDelete = emailProcessorService.processMessage(message);

        // Assert
        assertTrue(shouldDelete);
        verify(snippetService).createSnippetWithDate(eq(body), any(User.class), any(Instant.class));
    }

    @Test
    void processMessage_WithUnknownSender_ReturnsUserNotFound() throws Exception {
        // Arrange
        String senderEmail = "unknown@example.com";
        String messageId = "<unknown@example.com>";

        MimeMessage message = createMimeMessage(senderEmail, "Subject", "Body", messageId);

        when(processedEmailRepository.existsByMessageId(messageId)).thenReturn(false);
        when(userRepository.findByEmail(senderEmail)).thenReturn(Optional.empty());

        // Act
        boolean shouldDelete = emailProcessorService.processMessage(message);

        // Assert
        assertTrue(shouldDelete);
        verify(snippetService, never()).createSnippetWithDate(anyString(), any(), any());

        ArgumentCaptor<ProcessedEmail> captor = ArgumentCaptor.forClass(ProcessedEmail.class);
        verify(processedEmailRepository).save(captor.capture());
        assertEquals(ProcessingStatus.USER_NOT_FOUND, captor.getValue().getStatus());
    }

    @Test
    void processMessage_WithEmptyContent_ReturnsEmptyContent() throws Exception {
        // Arrange
        String senderEmail = "user@example.com";
        String messageId = "<empty@example.com>";

        MimeMessage message = createMimeMessageEmpty(senderEmail, messageId);

        User user = new User();
        user.setId(1L);
        user.setEmail(senderEmail);

        when(processedEmailRepository.existsByMessageId(messageId)).thenReturn(false);
        when(userRepository.findByEmail(senderEmail)).thenReturn(Optional.of(user));

        // Act
        boolean shouldDelete = emailProcessorService.processMessage(message);

        // Assert
        assertTrue(shouldDelete);
        verify(snippetService, never()).createSnippetWithDate(anyString(), any(), any());

        ArgumentCaptor<ProcessedEmail> captor = ArgumentCaptor.forClass(ProcessedEmail.class);
        verify(processedEmailRepository).save(captor.capture());
        assertEquals(ProcessingStatus.EMPTY_CONTENT, captor.getValue().getStatus());
    }

    @Test
    void processMessage_WithDuplicateMessageId_SkipsProcessing() throws Exception {
        // Arrange
        String messageId = "<duplicate@example.com>";
        MimeMessage message = createMimeMessage("user@example.com", "Subject", "Body", messageId);

        when(processedEmailRepository.existsByMessageId(messageId)).thenReturn(true);

        // Act
        boolean shouldDelete = emailProcessorService.processMessage(message);

        // Assert
        assertTrue(shouldDelete);
        verify(userRepository, never()).findByEmail(anyString());
        verify(snippetService, never()).createSnippetWithDate(anyString(), any(), any());
        verify(processedEmailRepository, never()).save(any());
    }

    @Test
    void processMessage_WithDatabaseError_LeavesForRetry() throws Exception {
        // Arrange
        String senderEmail = "user@example.com";
        String messageId = "<error@example.com>";

        MimeMessage message = createMimeMessage(senderEmail, "Subject", "Body", messageId);

        User user = new User();
        user.setId(1L);
        user.setEmail(senderEmail);

        when(processedEmailRepository.existsByMessageId(messageId)).thenReturn(false);
        when(userRepository.findByEmail(senderEmail)).thenReturn(Optional.of(user));
        when(snippetService.createSnippetWithDate(anyString(), any(User.class), any(Instant.class)))
                .thenThrow(new RuntimeException("Database error"));

        // Act
        boolean shouldDelete = emailProcessorService.processMessage(message);

        // Assert
        assertFalse(shouldDelete); // Should NOT delete - leave for retry

        ArgumentCaptor<ProcessedEmail> captor = ArgumentCaptor.forClass(ProcessedEmail.class);
        verify(processedEmailRepository).save(captor.capture());
        assertEquals(ProcessingStatus.PROCESSING_ERROR, captor.getValue().getStatus());
        assertTrue(captor.getValue().getErrorMessage().contains("Database error"));
    }

    @Test
    void processMessage_UsesReceivedDate_ForSnippetTimestamp() throws Exception {
        // Arrange
        String senderEmail = "user@example.com";
        String messageId = "<dated@example.com>";
        Date receivedDate = new Date(1700000000000L); // Fixed timestamp (jakarta.mail uses Date)

        MimeMessage message = createMimeMessage(senderEmail, "Subject", "Body", messageId);
        message.setHeader("Received", receivedDate.toString());

        User user = new User();
        user.setId(1L);
        user.setEmail(senderEmail);

        Snippet snippet = new Snippet();
        snippet.setId(103L);

        when(processedEmailRepository.existsByMessageId(messageId)).thenReturn(false);
        when(userRepository.findByEmail(senderEmail)).thenReturn(Optional.of(user));
        when(snippetService.createSnippetWithDate(anyString(), any(User.class), any(Instant.class)))
                .thenReturn(snippet);

        // Act
        emailProcessorService.processMessage(message);

        // Assert
        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(snippetService).createSnippetWithDate(anyString(), any(User.class), instantCaptor.capture());
        assertNotNull(instantCaptor.getValue());
    }

    /**
     * Helper method to create a MimeMessage for testing.
     */
    private MimeMessage createMimeMessage(String from, String subject, String body, String messageId)
            throws Exception {
        MimeMessage message = new MimeMessage(mailSession);
        message.setFrom(new InternetAddress(from));
        if (subject != null) {
            message.setSubject(subject);
        }
        if (body != null && !body.isEmpty()) {
            message.setText(body);
        }
        if (messageId != null) {
            message.setHeader("Message-ID", messageId);
        }
        message.setSentDate(new Date());
        return message;
    }

    /**
     * Helper method to create a MimeMessage with subject only (no body).
     */
    private MimeMessage createMimeMessageWithSubjectOnly(String from, String subject, String messageId)
            throws Exception {
        MimeMessage message = new MimeMessage(mailSession);
        message.setFrom(new InternetAddress(from));
        message.setSubject(subject);
        message.setHeader("Message-ID", messageId);
        message.setSentDate(new Date());
        // Don't set any content - this creates a message with subject only
        return message;
    }

    /**
     * Helper method to create an empty MimeMessage (no subject, no body).
     */
    private MimeMessage createMimeMessageEmpty(String from, String messageId)
            throws Exception {
        MimeMessage message = new MimeMessage(mailSession);
        message.setFrom(new InternetAddress(from));
        message.setHeader("Message-ID", messageId);
        message.setSentDate(new Date());
        // Don't set subject or content
        return message;
    }
}

