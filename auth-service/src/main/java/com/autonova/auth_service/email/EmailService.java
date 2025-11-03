package com.autonova.auth_service.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Email Service for sending emails
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.email.from-name}")
    private String fromName;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    /**
     * Send password reset email
     */
    public void sendPasswordResetEmail(String toEmail, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Password Reset Request - AutoNova");

            String resetLink = frontendUrl + "/reset-password?token=" + token;
            String htmlContent = buildPasswordResetEmailHtml(resetLink, token);

            helper.setText(htmlContent, true); // true = HTML

            mailSender.send(message);
            
            log.info("✅ Password reset email sent successfully to: {}", toEmail);
            
        } catch (MessagingException e) {
            log.error("❌ Failed to send password reset email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        } catch (Exception e) {
            log.error("❌ Unexpected error sending email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }

    /**
     * Build HTML email content for password reset
     */
    private String buildPasswordResetEmailHtml(String resetLink, String token) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        line-height: 1.6;
                        color: #333;
                        max-width: 600px;
                        margin: 0 auto;
                        padding: 20px;
                    }
                    .container {
                        background-color: #f9f9f9;
                        border-radius: 10px;
                        padding: 30px;
                        border: 1px solid #ddd;
                    }
                    .header {
                        text-align: center;
                        margin-bottom: 30px;
                    }
                    .header h1 {
                        color: #2563eb;
                        margin: 0;
                    }
                    .content {
                        background-color: white;
                        padding: 25px;
                        border-radius: 8px;
                        margin: 20px 0;
                    }
                    .button {
                        display: inline-block;
                        padding: 12px 30px;
                        background-color: #2563eb;
                        color: white !important;
                        text-decoration: none;
                        border-radius: 5px;
                        margin: 20px 0;
                        font-weight: bold;
                    }
                    .button:hover {
                        background-color: #1d4ed8;
                    }
                    .token-box {
                        background-color: #f3f4f6;
                        padding: 15px;
                        border-radius: 5px;
                        border-left: 4px solid #2563eb;
                        margin: 20px 0;
                        word-break: break-all;
                        font-family: monospace;
                    }
                    .warning {
                        background-color: #fef3c7;
                        padding: 15px;
                        border-radius: 5px;
                        border-left: 4px solid #f59e0b;
                        margin: 20px 0;
                    }
                    .footer {
                        text-align: center;
                        color: #666;
                        font-size: 12px;
                        margin-top: 30px;
                        padding-top: 20px;
                        border-top: 1px solid #ddd;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🔐 AutoNova</h1>
                        <p>Password Reset Request</p>
                    </div>
                    
                    <div class="content">
                        <h2>Hello,</h2>
                        
                        <p>We received a request to reset your password for your AutoNova account.</p>
                        
                        <p>Click the button below to reset your password:</p>
                        
                        <div style="text-align: center;">
                            <a href="%s" class="button">Reset Password</a>
                        </div>
                        
                        <p>Or copy and paste this link into your browser:</p>
                        <div class="token-box">%s</div>
                        
                        <div class="warning">
                            <strong>⏰ Important:</strong>
                            <ul style="margin: 10px 0;">
                                <li>This link will expire in <strong>24 hours</strong></li>
                                <li>This link can only be used <strong>once</strong></li>
                                <li>If you didn't request this, please ignore this email</li>
                            </ul>
                        </div>
                        
                        <p>For security reasons, we cannot tell you whether an account exists with your email address.</p>
                    </div>
                    
                    <div class="footer">
                        <p>This is an automated email from AutoNova. Please do not reply to this email.</p>
                        <p>&copy; 2025 AutoNova. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(resetLink, resetLink);
    }

    /**
     * Send welcome email (optional - can be used after registration)
     */
    public void sendWelcomeEmail(String toEmail, String userName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Welcome to AutoNova!");

            String htmlContent = buildWelcomeEmailHtml(userName);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            
            log.info("✅ Welcome email sent successfully to: {}", toEmail);
            
        } catch (Exception e) {
            log.error("❌ Failed to send welcome email to: {}", toEmail, e);
            // Don't throw exception for welcome email - it's not critical
        }
    }

    /**
     * Build HTML email content for welcome email
     */
    private String buildWelcomeEmailHtml(String userName) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        line-height: 1.6;
                        color: #333;
                        max-width: 600px;
                        margin: 0 auto;
                        padding: 20px;
                    }
                    .container {
                        background-color: #f9f9f9;
                        border-radius: 10px;
                        padding: 30px;
                        border: 1px solid #ddd;
                    }
                    .header {
                        text-align: center;
                        margin-bottom: 30px;
                    }
                    .header h1 {
                        color: #2563eb;
                        margin: 0;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🚗 Welcome to AutoNova!</h1>
                    </div>
                    <h2>Hello %s,</h2>
                    <p>Thank you for joining AutoNova! Your account has been created successfully.</p>
                    <p>You can now access all our automobile services.</p>
                    <p>Best regards,<br>The AutoNova Team</p>
                </div>
            </body>
            </html>
            """.formatted(userName);
    }
}
