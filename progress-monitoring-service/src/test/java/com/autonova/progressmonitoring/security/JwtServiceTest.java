package com.autonova.progressmonitoring.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Progress Monitoring Service - JwtService Unit Tests")
class JwtServiceTest {

    private JwtService jwtService;

    // Use a test secret key (base64 encoded, 256 bits minimum for HS256)
    private static final String TEST_SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtdG9rZW4tdGVzdGluZy1wdXJwb3Nlcy1vbmx5LW1pbmltdW0tMjU2LWJpdHM=";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", TEST_SECRET);
    }

    private String createTestToken(Map<String, Object> claims, String subject, long expirationMillis) {
        Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));
        Instant now = Instant.now();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(expirationMillis)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    @DisplayName("Should extract username from token")
    void extractUsername_FromValidToken_ShouldReturnUsername() {
        // Given
        String email = "test@example.com";
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 1L);
        claims.put("role", "CUSTOMER");
        String token = createTestToken(claims, email, 3600000L);

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
        claims.put("role", "EMPLOYEE");
        String token = createTestToken(claims, "test@example.com", 3600000L);

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
        String token = createTestToken(claims, "admin@example.com", 3600000L);

        // When
        String extractedRole = jwtService.extractRole(token);

        // Then
        assertThat(extractedRole).isEqualTo(role);
    }

    @Test
    @DisplayName("Should extract expiration date from token")
    void extractExpiration_FromValidToken_ShouldReturnExpirationDate() {
        // Given
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 1L);
        String token = createTestToken(claims, "test@example.com", 3600000L);

        // When
        Date expiration = jwtService.extractExpiration(token);

        // Then
        assertThat(expiration).isNotNull();
        assertThat(expiration).isAfter(new Date());
    }

    @Test
    @DisplayName("Should validate non-expired token")
    void validateToken_WithValidToken_ShouldReturnTrue() {
        // Given
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 1L);
        claims.put("role", "CUSTOMER");
        String token = createTestToken(claims, "test@example.com", 3600000L);

        // When
        Boolean isValid = jwtService.validateToken(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should not validate expired token")
    void validateToken_WithExpiredToken_ShouldReturnFalse() {
        // Given
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 1L);
        claims.put("role", "CUSTOMER");
        String token = createTestToken(claims, "test@example.com", -1000L); // Already expired

        // When
        Boolean isValid = jwtService.validateToken(token);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should return false for invalid token format")
    void validateToken_WithInvalidToken_ShouldReturnFalse() {
        // Given
        String invalidToken = "invalid.token.format";

        // When
        Boolean isValid = jwtService.validateToken(invalidToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should handle malformed token in extraction")
    void extractUsername_WithInvalidToken_ShouldThrowException() {
        // Given
        String invalidToken = "not-a-jwt-token";

        // When & Then
        assertThatThrownBy(() -> jwtService.extractUsername(invalidToken))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should extract all claims from token")
    void extractClaims_FromValidToken_ShouldExtractCorrectly() {
        // Given
        Long userId = 42L;
        String email = "user@example.com";
        String role = "EMPLOYEE";
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);
        String token = createTestToken(claims, email, 3600000L);

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
    @DisplayName("Should validate token with future expiration")
    void validateToken_WithFutureExpiration_ShouldReturnTrue() {
        // Given - token expires in 1 day
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 1L);
        String token = createTestToken(claims, "test@example.com", 86400000L);

        // When
        Boolean isValid = jwtService.validateToken(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should handle null userId in token")
    void extractUserId_WithNullUserId_ShouldReturnNull() {
        // Given
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "CUSTOMER");
        // userId not set
        String token = createTestToken(claims, "test@example.com", 3600000L);

        // When
        Long extractedUserId = jwtService.extractUserId(token);

        // Then
        assertThat(extractedUserId).isNull();
    }

    @Test
    @DisplayName("Should handle null role in token")
    void extractRole_WithNullRole_ShouldReturnNull() {
        // Given
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 1L);
        // role not set
        String token = createTestToken(claims, "test@example.com", 3600000L);

        // When
        String extractedRole = jwtService.extractRole(token);

        // Then
        assertThat(extractedRole).isNull();
    }

    @Test
    @DisplayName("Should validate token returns false for very old token")
    void validateToken_WithVeryOldToken_ShouldReturnFalse() {
        // Given - token expired 1 day ago
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 1L);
        String token = createTestToken(claims, "test@example.com", -86400000L);

        // When
        Boolean isValid = jwtService.validateToken(token);

        // Then
        assertThat(isValid).isFalse();
    }
}
