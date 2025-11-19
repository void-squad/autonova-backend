package com.autonova.customer.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Customer Service - JwtService Unit Tests")
class JwtServiceTest {

    private JwtService jwtService;

    // Use a test secret key (base64 encoded, 256 bits minimum for HS256)
    private static final String TEST_SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtdG9rZW4tdGVzdGluZy1wdXJwb3Nlcy1vbmx5LW1pbmltdW0tMjU2LWJpdHM=";
    private static final long TEST_EXPIRATION = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", TEST_EXPIRATION);
    }

    @Test
    @DisplayName("Should create JWT token with claims")
    void createToken_WithClaims_ShouldReturnToken() {
        // Given
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 1L);
        claims.put("role", "CUSTOMER");
        String subject = "test@example.com";

        // When
        String token = jwtService.createToken(claims, subject);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
    }

    @Test
    @DisplayName("Should extract username from token")
    void extractUsername_FromValidToken_ShouldReturnUsername() {
        // Given
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 1L);
        claims.put("role", "CUSTOMER");
        String email = "test@example.com";
        String token = jwtService.createToken(claims, email);

        // When
        String extractedUsername = jwtService.extractUsername(token);

        // Then
        assertThat(extractedUsername).isEqualTo(email);
    }

    @Test
    @DisplayName("Should extract user ID from token")
    void extractUserId_FromValidToken_ShouldReturnUserId() {
        // Given
        Long userId = 123L;
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", "CUSTOMER");
        String token = jwtService.createToken(claims, "test@example.com");

        // When
        Long extractedUserId = jwtService.extractUserId(token);

        // Then
        assertThat(extractedUserId).isEqualTo(userId);
    }

    @Test
    @DisplayName("Should extract role from token")
    void extractRole_FromValidToken_ShouldReturnRole() {
        // Given
        String role = "ADMIN";
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 1L);
        claims.put("role", role);
        String token = jwtService.createToken(claims, "admin@example.com");

        // When
        String extractedRole = jwtService.extractRole(token);

        // Then
        assertThat(extractedRole).isEqualTo(role);
    }

    @Test
    @DisplayName("Should validate token with correct username")
    void validateToken_WithCorrectUsername_ShouldReturnTrue() {
        // Given
        String email = "test@example.com";
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 1L);
        claims.put("role", "CUSTOMER");
        String token = jwtService.createToken(claims, email);

        // When
        Boolean isValid = jwtService.validateToken(token, email);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should not validate token with incorrect username")
    void validateToken_WithIncorrectUsername_ShouldReturnFalse() {
        // Given
        String email = "test@example.com";
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 1L);
        claims.put("role", "CUSTOMER");
        String token = jwtService.createToken(claims, email);

        // When
        Boolean isValid = jwtService.validateToken(token, "wrong@example.com");

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should not validate expired token")
    void validateToken_WithExpiredToken_ShouldThrowException() {
        // Given
        JwtService shortExpirationService = new JwtService();
        ReflectionTestUtils.setField(shortExpirationService, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(shortExpirationService, "expiration", -1000L); // Expired

        String email = "test@example.com";
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 1L);
        claims.put("role", "CUSTOMER");
        String token = shortExpirationService.createToken(claims, email);

        // When & Then - expired token throws exception during extraction
        assertThatThrownBy(() -> shortExpirationService.validateToken(token, email))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should handle invalid token format")
    void extractUsername_WithInvalidToken_ShouldThrowException() {
        // Given
        String invalidToken = "invalid.token.format";

        // When & Then
        assertThatThrownBy(() -> jwtService.extractUsername(invalidToken))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should extract all claims from token")
    void extractClaims_FromValidToken_ShouldExtractCorrectly() {
        // Given
        Long userId = 42L;
        String email = "custom@example.com";
        String role = "EMPLOYEE";
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);
        String token = jwtService.createToken(claims, email);

        // When
        String extractedEmail = jwtService.extractUsername(token);
        Long extractedUserId = jwtService.extractUserId(token);
        String extractedRole = jwtService.extractRole(token);

        // Then
        assertThat(extractedEmail).isEqualTo(email);
        assertThat(extractedUserId).isEqualTo(userId);
        assertThat(extractedRole).isEqualTo(role);
    }

    @Test
    @DisplayName("Should generate different tokens for different users")
    void createToken_ForDifferentUsers_ShouldGenerateDifferentTokens() {
        // Given
        Map<String, Object> claims1 = new HashMap<>();
        claims1.put("userId", 1L);
        claims1.put("role", "CUSTOMER");
        
        Map<String, Object> claims2 = new HashMap<>();
        claims2.put("userId", 2L);
        claims2.put("role", "CUSTOMER");

        // When
        String token1 = jwtService.createToken(claims1, "user1@example.com");
        String token2 = jwtService.createToken(claims2, "user2@example.com");

        // Then
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    @DisplayName("Should create tokens with correct structure")
    void createToken_ShouldHaveCorrectStructure() {
        // Given
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 1L);
        claims.put("role", "CUSTOMER");

        // When
        String token = jwtService.createToken(claims, "test@example.com");

        // Then
        String[] parts = token.split("\\.");
        assertThat(parts).hasSize(3);
        assertThat(parts[0]).isNotEmpty(); // header
        assertThat(parts[1]).isNotEmpty(); // payload
        assertThat(parts[2]).isNotEmpty(); // signature
    }

    @Test
    @DisplayName("Should validate token returns false for null username")
    void validateToken_WithNullExtractedUsername_ShouldReturnFalse() {
        // Given
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 1L);
        claims.put("role", "CUSTOMER");
        String token = jwtService.createToken(claims, "test@example.com");

        // When - passing null as expected username
        Boolean isValid = jwtService.validateToken(token, null);

        // Then
        assertThat(isValid).isFalse();
    }
}
