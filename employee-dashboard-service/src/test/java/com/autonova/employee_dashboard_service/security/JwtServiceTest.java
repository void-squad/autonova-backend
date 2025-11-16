package com.autonova.employee_dashboard_service.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    private JwtService jwtService;
    private String testSecret;
    private String testToken;
    private final String testUsername = "test@autonova.com";
    private final Long testUserId = 123L;
    private final String testRole = "EMPLOYEE";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Use a test secret (must be at least 256 bits for HS256)
        testSecret = "dGhpc2lzYXZlcnlsb25nc2VjcmV0a2V5Zm9ydGVzdGluZ3B1cnBvc2VzMTIzNDU2Nzg5MA==";
        ReflectionTestUtils.setField(jwtService, "secret", testSecret);

        // Create a valid test token
        testToken = createTestToken(testUsername, testUserId, testRole, System.currentTimeMillis() + 3600000);
    }

    private String createTestToken(String username, Long userId, String role, long expirationTime) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);

        byte[] keyBytes = Decoders.BASE64.decode(testSecret);
        Key key = Keys.hmacShaKeyFor(keyBytes);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(expirationTime))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    @DisplayName("Should extract username from valid token")
    void shouldExtractUsername() {
        // When
        String username = jwtService.extractUsername(testToken);

        // Then
        assertThat(username).isEqualTo(testUsername);
    }

    @Test
    @DisplayName("Should extract userId from valid token")
    void shouldExtractUserId() {
        // When
        Long userId = jwtService.extractUserId(testToken);

        // Then
        assertThat(userId).isEqualTo(testUserId);
    }

    @Test
    @DisplayName("Should extract role from valid token")
    void shouldExtractRole() {
        // When
        String role = jwtService.extractRole(testToken);

        // Then
        assertThat(role).isEqualTo(testRole);
    }

    @Test
    @DisplayName("Should extract expiration date from token")
    void shouldExtractExpiration() {
        // When
        Date expiration = jwtService.extractExpiration(testToken);

        // Then
        assertNotNull(expiration);
        assertThat(expiration).isAfter(new Date());
    }

    @Test
    @DisplayName("Should validate valid token")
    void shouldValidateValidToken() {
        // When
        Boolean isValid = jwtService.validateToken(testToken);

        // Then
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should reject expired token")
    void shouldRejectExpiredToken() {
        // Given - create an expired token
        String expiredToken = createTestToken(testUsername, testUserId, testRole, System.currentTimeMillis() - 1000);

        // When
        Boolean isValid = jwtService.validateToken(expiredToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should reject invalid token format")
    void shouldRejectInvalidTokenFormat() {
        // Given
        String invalidToken = "invalid.token.format";

        // When
        Boolean isValid = jwtService.validateToken(invalidToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should reject token with invalid signature")
    void shouldRejectTokenWithInvalidSignature() {
        // Given - create token with different secret
        String differentSecret = "ZGlmZmVyZW50c2VjcmV0a2V5Zm9ydGVzdGluZ3B1cnBvc2VzMTIzNDU2Nzg5MA==";
        byte[] keyBytes = Decoders.BASE64.decode(differentSecret);
        Key differentKey = Keys.hmacShaKeyFor(keyBytes);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", testUserId);
        claims.put("role", testRole);

        String tokenWithDifferentSignature = Jwts.builder()
                .setClaims(claims)
                .setSubject(testUsername)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(differentKey, SignatureAlgorithm.HS256)
                .compact();

        // When
        Boolean isValid = jwtService.validateToken(tokenWithDifferentSignature);

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should handle null token gracefully")
    void shouldHandleNullToken() {
        // When
        Boolean isValid = jwtService.validateToken(null);

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should extract correct claims from token with multiple roles")
    void shouldExtractClaimsFromTokenWithManagerRole() {
        // Given
        String managerToken = createTestToken("manager@autonova.com", 456L, "MANAGER", System.currentTimeMillis() + 3600000);

        // When
        String username = jwtService.extractUsername(managerToken);
        Long userId = jwtService.extractUserId(managerToken);
        String role = jwtService.extractRole(managerToken);

        // Then
        assertThat(username).isEqualTo("manager@autonova.com");
        assertThat(userId).isEqualTo(456L);
        assertThat(role).isEqualTo("MANAGER");
    }

    @Test
    @DisplayName("Should validate token is not expired when just created")
    void shouldValidateRecentlyCreatedToken() {
        // Given - token that expires in 1 hour
        String recentToken = createTestToken(testUsername, testUserId, testRole, System.currentTimeMillis() + 3600000);

        // When
        Boolean isValid = jwtService.validateToken(recentToken);

        // Then
        assertTrue(isValid);
    }
}
