package com.autonova.auth_service.security.repository;

import com.autonova.auth_service.security.model.RefreshToken;
import com.autonova.auth_service.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * Repository for Refresh Token operations
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Find refresh token by token string
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Find all refresh tokens for a user
     */
    Optional<RefreshToken> findByUser(User user);

    /**
     * Delete all refresh tokens for a user (logout all devices)
     */
    void deleteByUser(User user);

    /**
     * Delete expired refresh tokens
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiryDate < :now")
    void deleteExpiredTokens(Instant now);

    /**
     * Delete revoked tokens
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.revoked = true")
    void deleteRevokedTokens();
}
