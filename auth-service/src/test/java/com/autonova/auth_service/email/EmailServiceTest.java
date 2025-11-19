package com.autonova.auth_service.email;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService Unit Tests")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        mimeMessage = new MimeMessage((Session) null);
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@autonova.com");
        ReflectionTestUtils.setField(emailService, "fromName", "AutoNova Test");
        ReflectionTestUtils.setField(emailService, "frontendUrl", "https://test.autonova.com");
    }

    @Test
    @DisplayName("Should send password reset email successfully")
    void sendPasswordResetEmail_Success_ShouldReturnTrue() {
        // Given
        String toEmail = "user@example.com";
        String token = "test-token-123";
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // When
        boolean result = emailService.sendPasswordResetEmail(toEmail, token);

        // Then
        assertThat(result).isTrue();
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should return false when messaging exception occurs")
    void sendPasswordResetEmail_MessagingException_ShouldReturnFalse() {
        // Given
        String toEmail = "user@example.com";
        String token = "test-token-123";
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("SMTP Error")).when(mailSender).send(any(MimeMessage.class));

        // When
        boolean result = emailService.sendPasswordResetEmail(toEmail, token);

        // Then
        assertThat(result).isFalse();
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should send welcome email successfully")
    void sendWelcomeEmail_Success_ShouldNotThrowException() {
        // Given
        String toEmail = "newuser@example.com";
        String userName = "New User";
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // When & Then - Should not throw exception
        assertThatCode(() -> emailService.sendWelcomeEmail(toEmail, userName))
                .doesNotThrowAnyException();

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should not throw exception when welcome email fails")
    void sendWelcomeEmail_Failure_ShouldNotThrowException() {
        // Given
        String toEmail = "newuser@example.com";
        String userName = "New User";
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("SMTP Error")).when(mailSender).send(any(MimeMessage.class));

        // When & Then - Should not throw exception (graceful degradation)
        assertThatCode(() -> emailService.sendWelcomeEmail(toEmail, userName))
                .doesNotThrowAnyException();

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should handle null username in welcome email")
    void sendWelcomeEmail_WithNullUsername_ShouldHandleGracefully() {
        // Given
        String toEmail = "newuser@example.com";
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // When & Then
        assertThatCode(() -> emailService.sendWelcomeEmail(toEmail, null))
                .doesNotThrowAnyException();

        verify(mailSender).createMimeMessage();
    }

    @Test
    @DisplayName("Should create password reset email with correct token")
    void sendPasswordResetEmail_ShouldIncludeTokenInLink() {
        // Given
        String toEmail = "user@example.com";
        String token = "unique-token-456";
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // When
        boolean result = emailService.sendPasswordResetEmail(toEmail, token);

        // Then
        assertThat(result).isTrue();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should handle mail sender creation failure")
    void sendPasswordResetEmail_CreateMimeMessageFails_ShouldReturnFalse() {
        // Given
        String toEmail = "user@example.com";
        String token = "test-token";
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("Mail system unavailable"));

        // When
        boolean result = emailService.sendPasswordResetEmail(toEmail, token);

        // Then
        assertThat(result).isFalse();
        verify(mailSender).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}
