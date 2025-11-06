package com.autonova.auth_service.security.service;

import com.autonova.auth_service.security.model.RefreshToken;
import com.autonova.auth_service.security.repository.RefreshTokenRepository;
import com.autonova.auth_service.user.model.User;
import com.autonova.auth_service.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Refresh Token Service
 * Handles creation, validation, and refresh of JWT tokens
 */
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Value("${jwt.refresh.expiration}")
    private Long refreshTokenDurationMs;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    /**
     * Create refresh token for a user
     */
    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        // Delete any existing refresh tokens for this user
        refreshTokenRepository.deleteByUser(user);

        // Create new refresh token
        String token = UUID.randomUUID().toString();
        Instant expiryDate = Instant.now().plus(refreshTokenDurationMs, ChronoUnit.MILLIS);

        RefreshToken refreshToken = new RefreshToken(token, user, expiryDate);
        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Find refresh token by token string
     */
    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token not found"));
    }

    /**
     * Verify refresh token expiration
     */
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            throw new IllegalArgumentException("Refresh token has expired. Please login again.");
        }
        return token;
    }

    /**
     * Revoke refresh token (logout)
     */
    @Transactional
    public void revokeToken(String token) {
        RefreshToken refreshToken = findByToken(token);
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }

    /**
     * Revoke all tokens for a user (logout from all devices)
     */
    @Transactional
    public void revokeAllUserTokens(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        refreshTokenRepository.deleteByUser(user);
    }

    /**
     * Clean up expired tokens (can be scheduled)
     */
    @Transactional
    public void deleteExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(Instant.now());
    }

    /**
     * Clean up revoked tokens (can be scheduled)
     */
    @Transactional
    public void deleteRevokedTokens() {
        refreshTokenRepository.deleteRevokedTokens();
    }
}
