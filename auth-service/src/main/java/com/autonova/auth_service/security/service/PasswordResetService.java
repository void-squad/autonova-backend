package com.autonova.auth_service.security.service;

import com.autonova.auth_service.email.EmailService;
import com.autonova.auth_service.security.model.PasswordResetToken;
import com.autonova.auth_service.security.repository.PasswordResetTokenRepository;
import com.autonova.auth_service.user.model.User;
import com.autonova.auth_service.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * Password Reset Service
 * Handles forgot password and reset password functionality
 */
@Service
@Slf4j
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    
    // Token expiry time in hours (2 hours for enhanced security)
    private static final int TOKEN_EXPIRY_HOURS = 2;

    public PasswordResetService(
            PasswordResetTokenRepository tokenRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * Generate password reset token for a user
     * @param email User's email
     * @return Generated token string, or null if email doesn't exist (security: don't reveal if email exists)
     */
    @Transactional
    public String generateResetToken(String email) {
        log.info("🔑 Password reset requested for email: {}", email);
        
        // Security best practice: Check if email exists first
        // Don't reveal to user if email exists or not (prevents user enumeration attacks)
        Optional<User> userOptional = userRepository.findByEmail(email);
        
        if (userOptional.isEmpty()) {
            log.warn("⚠️ Password reset requested for non-existent email: {}", email);
            log.info("🔒 For security, user will see generic success message (don't reveal email doesn't exist)");
            return null; // Return null but show success message to user (security best practice)
        }
        
        User user = userOptional.get();
        log.info("✅ User found: {} (ID: {})", user.getEmail(), user.getId());

        // Delete any existing tokens for this user
        tokenRepository.deleteByUser(user);
        log.info("🗑️ Deleted existing password reset tokens for user: {}", user.getEmail());

        // Generate random token
        String token = UUID.randomUUID().toString();
        log.info("🎫 Generated new reset token: {}", token.substring(0, 8) + "...");

        // Calculate expiry date (2 hours from now)
        Instant expiryDate = Instant.now().plus(TOKEN_EXPIRY_HOURS, ChronoUnit.HOURS);

        // Create and save token
        PasswordResetToken resetToken = new PasswordResetToken(token, user, expiryDate);
        tokenRepository.save(resetToken);
        log.info("💾 Saved password reset token (expires in {} hours)", TOKEN_EXPIRY_HOURS);

        // Send email with token (won't throw exception even if email fails)
        boolean emailSent = emailService.sendPasswordResetEmail(user.getEmail(), token);
        
        if (emailSent) {
            log.info("✅ Password reset process completed successfully for: {}", email);
        } else {
            log.warn("⚠️ Token generated but email not sent for: {}. Check application.properties for email config", email);
        }

        return token;
    }

    /**
     * Validate reset token and reset password
     * @param token Reset token
     * @param newPassword New password (plain text - will be hashed)
     * @throws IllegalArgumentException if token is invalid, expired, or already used
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        // Validate input
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("New password is required");
        }

        // Find token
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid reset token"));

        // Check if token is expired
        if (resetToken.isExpired()) {
            throw new IllegalArgumentException("Reset token has expired");
        }

        // Check if token is already used
        if (resetToken.isUsed()) {
            throw new IllegalArgumentException("Reset token has already been used");
        }

        // Get user and update password
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        System.out.println("✅ Password reset successful for user: " + user.getEmail());
    }

    /**
     * Validate if a token exists and is valid
     * @param token Reset token
     * @return true if token is valid and not expired
     */
    public boolean validateToken(String token) {
        Optional<PasswordResetToken> resetToken = tokenRepository.findByToken(token);
        
        if (resetToken.isEmpty()) {
            return false;
        }

        PasswordResetToken tokenEntity = resetToken.get();
        return !tokenEntity.isExpired() && !tokenEntity.isUsed();
    }

    /**
     * Clean up expired tokens (can be scheduled to run periodically)
     */
    @Transactional
    public void deleteExpiredTokens() {
        tokenRepository.deleteExpiredTokens(Instant.now());
        System.out.println("🧹 Expired password reset tokens cleaned up");
    }
}
