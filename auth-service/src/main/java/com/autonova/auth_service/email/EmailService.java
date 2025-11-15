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
     * Returns true if sent successfully, false if failed
     */
    public boolean sendPasswordResetEmail(String toEmail, String token) {
        try {
            log.info("📧 Attempting to send password reset email to: {}", toEmail);
            log.info("🔧 Email configuration - From: {}, SMTP Host: {}", fromEmail, mailSender);
            
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
            log.info("🔗 Reset link: {}", resetLink);
            return true;
            
        } catch (MessagingException e) {
            log.error("❌ Failed to send password reset email to: {}", toEmail, e);
            log.error("💡 Email sending failed - Check SMTP credentials in application.properties");
            log.error("💡 For Gmail: Generate App Password at https://myaccount.google.com/apppasswords");
            log.warn("⚠️ Email not sent, but token generated. Manual reset link: {}/reset-password?token={}", 
                    frontendUrl, token);
            return false;
        } catch (Exception e) {
            log.error("❌ Unexpected error sending email to: {}", toEmail, e);
            log.warn("⚠️ Email not sent, but token generated. Manual reset link: {}/reset-password?token={}", 
                    frontendUrl, token);
            return false;
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
                        
                        <div class="warning">
                            <strong>⏰ Important:</strong>
                            <ul style="margin: 10px 0;">
                                <li>This link will expire in <strong>2 hours</strong></li>
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
            """.formatted(resetLink);
    }

    /**
     * Send welcome email
     * Automatically sent after successful user registration
     */
    public void sendWelcomeEmail(String toEmail, String userName) {
        try {
            log.info("📧 Attempting to send welcome email to: {} ({})", toEmail, userName);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Welcome to AutoNova - Your Account is Ready!");

            String htmlContent = buildWelcomeEmailHtml(userName);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            
            log.info("✅ Welcome email sent successfully to: {} ({})", toEmail, userName);
            
        } catch (Exception e) {
            log.error("❌ Failed to send welcome email to: {} ({})", toEmail, userName, e);
            // Don't throw exception for welcome email - it's not critical for registration
            log.warn("⚠️ Welcome email not sent, but user registration completed successfully");
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
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        line-height: 1.6;
                        color: #333;
                        max-width: 600px;
                        margin: 0 auto;
                        padding: 20px;
                        background-color: #f5f5f5;
                    }
                    .container {
                        background-color: #ffffff;
                        border-radius: 10px;
                        padding: 40px 30px;
                        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
                    }
                    .header {
                        text-align: center;
                        margin-bottom: 30px;
                        padding-bottom: 20px;
                        border-bottom: 2px solid #2563eb;
                    }
                    .header h1 {
                        color: #2563eb;
                        margin: 0;
                        font-size: 28px;
                    }
                    .header p {
                        color: #666;
                        margin: 10px 0 0 0;
                        font-size: 16px;
                    }
                    .content {
                        padding: 20px 0;
                    }
                    .content h2 {
                        color: #2563eb;
                        margin: 0 0 15px 0;
                        font-size: 22px;
                    }
                    .content p {
                        margin: 15px 0;
                        font-size: 15px;
                        line-height: 1.8;
                    }
                    .welcome-box {
                        background: linear-gradient(135deg, #2563eb 0%%, #1d4ed8 100%%);
                        color: white;
                        padding: 20px;
                        border-radius: 8px;
                        text-align: center;
                        margin: 25px 0;
                    }
                    .welcome-box h3 {
                        margin: 0 0 10px 0;
                        font-size: 20px;
                    }
                    .features {
                        background-color: #f8fafc;
                        padding: 20px;
                        border-radius: 8px;
                        margin: 25px 0;
                    }
                    .features h3 {
                        color: #2563eb;
                        margin: 0 0 15px 0;
                        font-size: 18px;
                    }
                    .features ul {
                        margin: 0;
                        padding-left: 20px;
                    }
                    .features li {
                        margin: 10px 0;
                        color: #555;
                    }
                    .cta-button {
                        display: inline-block;
                        padding: 14px 35px;
                        background-color: #2563eb;
                        color: white !important;
                        text-decoration: none;
                        border-radius: 6px;
                        margin: 25px 0;
                        font-weight: bold;
                        font-size: 16px;
                        transition: background-color 0.3s;
                    }
                    .cta-button:hover {
                        background-color: #1d4ed8;
                    }
                    .button-container {
                        text-align: center;
                    }
                    .footer {
                        text-align: center;
                        color: #666;
                        font-size: 13px;
                        margin-top: 30px;
                        padding-top: 20px;
                        border-top: 1px solid #e5e7eb;
                    }
                    .footer p {
                        margin: 5px 0;
                    }
                    .highlight {
                        color: #2563eb;
                        font-weight: bold;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🚗 AutoNova</h1>
                        <p>Your Trusted Automobile Service Partner</p>
                    </div>
                    
                    <div class="content">
                        <div class="welcome-box">
                            <h3>🎉 Welcome Aboard!</h3>
                            <p style="margin: 0;">Your account has been successfully created</p>
                        </div>
                        
                        <h2>Hello <span class="highlight">%s</span>,</h2>
                        
                        <p>Thank you for joining <strong>AutoNova</strong>! We're excited to have you as part of our community.</p>
                        
                        <p>Your account is now active, and you can start exploring all the amazing features we offer for your automobile service needs.</p>
                        
                        <div class="features">
                            <h3>✨ What You Can Do:</h3>
                            <ul>
                                <li>📅 <strong>Book Appointments</strong> - Schedule service appointments at your convenience</li>
                                <li>🔧 <strong>Service Tracking</strong> - Monitor your vehicle service progress in real-time</li>
                                <li>📊 <strong>Service History</strong> - Access complete records of all your services</li>
                                <li>💳 <strong>Easy Payments</strong> - Secure and convenient payment options</li>
                                <li>🔔 <strong>Notifications</strong> - Get timely updates about your vehicle services</li>
                                <li>👤 <strong>Profile Management</strong> - Update your details anytime</li>
                            </ul>
                        </div>
                        
                        <p>Ready to get started? Log in to your account now and explore all the features we have to offer!</p>
                        
                        <div class="button-container">
                            <a href="%s" class="cta-button">Go to Dashboard</a>
                        </div>
                        
                        <p style="margin-top: 30px;">If you have any questions or need assistance, our support team is always here to help.</p>
                        
                        <p style="margin-bottom: 0;"><strong>Best regards,</strong><br>The AutoNova Team</p>
                    </div>
                    
                    <div class="footer">
                        <p>This is an automated email from AutoNova. Please do not reply to this email.</p>
                        <p>If you didn't create this account, please contact our support team immediately.</p>
                        <p>&copy; 2025 AutoNova. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName, frontendUrl);
    }
}
