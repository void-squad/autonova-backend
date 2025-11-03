package com.autonova.auth_service.security.service;

import com.autonova.auth_service.email.EmailService;
import com.autonova.auth_service.security.model.PasswordResetToken;
import com.autonova.auth_service.security.repository.PasswordResetTokenRepository;
import com.autonova.auth_service.user.model.User;
import com.autonova.auth_service.user.repository.UserRepository;
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
     * @return Generated token string
     * @throws IllegalArgumentException if user not found
     */
    @Transactional
    public String generateResetToken(String email) {
        // Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

        // Delete any existing tokens for this user
        tokenRepository.deleteByUser(user);

        // Generate random token
        String token = UUID.randomUUID().toString();

        // Calculate expiry date (24 hours from now)
        Instant expiryDate = Instant.now().plus(TOKEN_EXPIRY_HOURS, ChronoUnit.HOURS);

        // Create and save token
        PasswordResetToken resetToken = new PasswordResetToken(token, user, expiryDate);
        tokenRepository.save(resetToken);

        // Send real email with token
        emailService.sendPasswordResetEmail(user.getEmail(), token);

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
