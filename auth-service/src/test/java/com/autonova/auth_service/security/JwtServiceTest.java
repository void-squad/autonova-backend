package com.autonova.auth_service.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtService Unit Tests")
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
    @DisplayName("Should generate JWT token with user details")
    void generateToken_WithUserDetails_ShouldReturnToken() {
        // Given
        Long userId = 1L;
        String email = "test@example.com";
        String role = "CUSTOMER";
        String firstName = "Test";

        // When
        String token = jwtService.generateToken(userId, email, role, firstName);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts: header.payload.signature
    }

    @Test
    @DisplayName("Should generate token with null firstName")
    void generateToken_WithNullFirstName_ShouldGenerateTokenWithEmptyString() {
        // Given
        Long userId = 1L;
        String email = "test@example.com";
        String role = "CUSTOMER";

        // When
        String token = jwtService.generateToken(userId, email, role, null);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        String extractedFirstName = jwtService.extractFirstName(token);
        assertThat(extractedFirstName).isEmpty();
    }

    @Test
    @DisplayName("Should extract username from token")
    void extractUsername_FromValidToken_ShouldReturnUsername() {
        // Given
        String email = "test@example.com";
        String token = jwtService.generateToken(1L, email, "CUSTOMER", "Test");

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
        String token = jwtService.generateToken(userId, "test@example.com", "CUSTOMER", "Test");

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
        String token = jwtService.generateToken(1L, "admin@example.com", role, "Admin");

        // When
        String extractedRole = jwtService.extractRole(token);

        // Then
        assertThat(extractedRole).isEqualTo(role);
    }

    @Test
    @DisplayName("Should extract first name from token")
    void extractFirstName_FromValidToken_ShouldReturnFirstName() {
        // Given
        String firstName = "John";
        String token = jwtService.generateToken(1L, "john@example.com", "CUSTOMER", firstName);

        // When
        String extractedFirstName = jwtService.extractFirstName(token);

        // Then
        assertThat(extractedFirstName).isEqualTo(firstName);
    }

    @Test
    @DisplayName("Should extract expiration date from token")
    void extractExpiration_FromValidToken_ShouldReturnExpirationDate() {
        // Given
        String token = jwtService.generateToken(1L, "test@example.com", "CUSTOMER", "Test");

        // When
        Date expiration = jwtService.extractExpiration(token);

        // Then
        assertThat(expiration).isNotNull();
        assertThat(expiration).isAfter(new Date());
    }

    @Test
    @DisplayName("Should validate token with correct username")
    void validateToken_WithCorrectUsername_ShouldReturnTrue() {
        // Given
        String email = "test@example.com";
        String token = jwtService.generateToken(1L, email, "CUSTOMER", "Test");

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
        String token = jwtService.generateToken(1L, email, "CUSTOMER", "Test");

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
        String token = shortExpirationService.generateToken(1L, email, "CUSTOMER", "Test");

        // When & Then - expired token throws exception during extraction
        assertThatThrownBy(() -> shortExpirationService.validateToken(token, email))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should handle invalid token format gracefully")
    void extractUsername_WithInvalidToken_ShouldThrowException() {
        // Given
        String invalidToken = "invalid.token.format";

        // When & Then
        assertThatThrownBy(() -> jwtService.extractUsername(invalidToken))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should extract all claims from token")
    void extractClaim_FromValidToken_ShouldExtractCorrectly() {
        // Given
        Long userId = 42L;
        String email = "custom@example.com";
        String role = "EMPLOYEE";
        String firstName = "Custom";
        String token = jwtService.generateToken(userId, email, role, firstName);

        // When
        String extractedEmail = jwtService.extractUsername(token);
        Long extractedUserId = jwtService.extractUserId(token);
        String extractedRole = jwtService.extractRole(token);
        String extractedFirstName = jwtService.extractFirstName(token);

        // Then
        assertThat(extractedEmail).isEqualTo(email);
        assertThat(extractedUserId).isEqualTo(userId);
        assertThat(extractedRole).isEqualTo(role);
        assertThat(extractedFirstName).isEqualTo(firstName);
    }

    @Test
    @DisplayName("Should generate different tokens for different users")
    void generateToken_ForDifferentUsers_ShouldGenerateDifferentTokens() {
        // Given
        String token1 = jwtService.generateToken(1L, "user1@example.com", "CUSTOMER", "User1");
        String token2 = jwtService.generateToken(2L, "user2@example.com", "CUSTOMER", "User2");

        // When & Then
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    @DisplayName("Should generate tokens with correct expiration time")
    void generateToken_ShouldSetCorrectExpirationTime() {
        // Given
        long beforeGeneration = System.currentTimeMillis();
        String token = jwtService.generateToken(1L, "test@example.com", "CUSTOMER", "Test");
        long afterGeneration = System.currentTimeMillis();

        // When
        Date expiration = jwtService.extractExpiration(token);

        // Then
        long expectedExpiration = beforeGeneration + TEST_EXPIRATION;
        long actualExpiration = expiration.getTime();
        
        // Allow for small timing differences (within 1 second)
        assertThat(actualExpiration).isBetween(expectedExpiration - 1000, afterGeneration + TEST_EXPIRATION + 1000);
    }
}
