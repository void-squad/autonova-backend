package com.voidsquad.chatbot.service.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class AuthHeaderDecoderServiceTest {

    private AuthHeaderDecoderService service;

    @BeforeEach
    void setUp() {
        service = new AuthHeaderDecoderService();
    }

    @Test
    void decode_ShouldReturnInvalidForNullHeader() {
        // Act
        AuthInfo result = service.decode(null);

        // Assert
        assertNotNull(result);
        assertFalse(result.isValid());
        assertEquals("Authorization header is missing", result.getError());
    }

    @Test
    void decode_ShouldReturnInvalidForBlankHeader() {
        // Act
        AuthInfo result = service.decode("   ");

        // Assert
        assertNotNull(result);
        assertFalse(result.isValid());
        assertEquals("Authorization header is missing", result.getError());
    }

    @Test
    void decode_ShouldReturnInvalidForMalformedHeader() {
        // Act
        AuthInfo result = service.decode("InvalidHeaderFormat");

        // Assert
        assertNotNull(result);
        assertFalse(result.isValid());
        assertEquals("Invalid Authorization header format", result.getError());
    }

    @Test
    void decode_ShouldReturnInvalidForUnsupportedScheme() {
        // Act
        AuthInfo result = service.decode("Basic sometoken");

        // Assert
        assertNotNull(result);
        assertFalse(result.isValid());
        assertEquals("Basic", result.getScheme());
        assertTrue(result.getError().contains("Unsupported scheme"));
    }

    @Test
    void decode_ShouldDecodeValidJWT() {
        // Arrange - Create a valid JWT-like structure
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"userId\":123,\"email\":\"test@example.com\",\"role\":\"ADMIN\",\"firstName\":\"John\"}".getBytes());
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{}".getBytes());
        String signature = Base64.getUrlEncoder().withoutPadding().encodeToString("signature".getBytes());
        String jwt = header + "." + payload + "." + signature;
        String authHeader = "Bearer " + jwt;

        // Act
        AuthInfo result = service.decode(authHeader);

        // Assert
        assertNotNull(result);
        assertTrue(result.isValid());
        assertEquals("Bearer", result.getScheme());
        assertEquals(123L, result.getUserId());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("ADMIN", result.getRole());
        assertEquals("John", result.getFirstName());
        assertNull(result.getError());
    }

    @Test
    void decode_ShouldHandleUserIdAsString() {
        // Arrange
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"userId\":\"456\",\"email\":\"test2@example.com\"}".getBytes());
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{}".getBytes());
        String signature = Base64.getUrlEncoder().withoutPadding().encodeToString("sig".getBytes());
        String jwt = header + "." + payload + "." + signature;

        // Act
        AuthInfo result = service.decode("Bearer " + jwt);

        // Assert
        assertTrue(result.isValid());
        assertEquals(456L, result.getUserId());
    }

    @Test
    void decode_ShouldHandleRolesFieldAlternative() {
        // Arrange
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"userId\":789,\"roles\":\"USER\"}".getBytes());
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{}".getBytes());
        String signature = Base64.getUrlEncoder().withoutPadding().encodeToString("sig".getBytes());
        String jwt = header + "." + payload + "." + signature;

        // Act
        AuthInfo result = service.decode("Bearer " + jwt);

        // Assert
        assertTrue(result.isValid());
        assertEquals("USER", result.getRole());
    }

    @Test
    void decode_ShouldHandleGivenNameFieldAlternative() {
        // Arrange
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"userId\":111,\"given_name\":\"Jane\"}".getBytes());
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{}".getBytes());
        String signature = Base64.getUrlEncoder().withoutPadding().encodeToString("sig".getBytes());
        String jwt = header + "." + payload + "." + signature;

        // Act
        AuthInfo result = service.decode("Bearer " + jwt);

        // Assert
        assertTrue(result.isValid());
        assertEquals("Jane", result.getFirstName());
    }

    @Test
    void decode_ShouldReturnInvalidForInvalidJWT() {
        // Act
        AuthInfo result = service.decode("Bearer invalidtoken");

        // Assert
        assertNotNull(result);
        assertFalse(result.isValid());
        assertTrue(result.getError().contains("Token does not appear to be a JWT"));
    }

    @Test
    void decode_ShouldReturnInvalidForMalformedJSON() {
        // Arrange
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{invalid json}".getBytes());
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{}".getBytes());
        String jwt = header + "." + payload + ".sig";

        // Act
        AuthInfo result = service.decode("Bearer " + jwt);

        // Assert
        assertNotNull(result);
        assertFalse(result.isValid());
        assertTrue(result.getError().contains("Failed to decode token payload"));
    }

    @Test
    void decode_ShouldHandleCaseInsensitiveBearer() {
        // Arrange
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"userId\":999}".getBytes());
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{}".getBytes());
        String jwt = header + "." + payload + ".sig";

        // Act
        AuthInfo result = service.decode("bearer " + jwt);

        // Assert
        assertTrue(result.isValid());
        assertEquals(999L, result.getUserId());
    }

    @Test
    void extractUserId_ShouldReturnNullForNullAuthInfo() {
        // Act
        Long userId = service.extractUserId(null);

        // Assert
        assertNull(userId);
    }

    @Test
    void extractUserId_ShouldReturnNullForNullClaims() {
        // Arrange
        AuthInfo authInfo = AuthInfo.builder().claims(null).build();

        // Act
        Long userId = service.extractUserId(authInfo);

        // Assert
        assertNull(userId);
    }

    @Test
    void extractUserId_ShouldExtractNumericUserId() {
        // Arrange
        AuthInfo authInfo = AuthInfo.builder()
                .claims(java.util.Map.of("userId", 123))
                .build();

        // Act
        Long userId = service.extractUserId(authInfo);

        // Assert
        assertEquals(123L, userId);
    }

    @Test
    void extractEmail_ShouldReturnNullForNullAuthInfo() {
        // Act
        String email = service.extractEmail(null);

        // Assert
        assertNull(email);
    }

    @Test
    void extractEmail_ShouldExtractEmail() {
        // Arrange
        AuthInfo authInfo = AuthInfo.builder()
                .claims(java.util.Map.of("email", "user@example.com"))
                .build();

        // Act
        String email = service.extractEmail(authInfo);

        // Assert
        assertEquals("user@example.com", email);
    }

    @Test
    void extractRole_ShouldExtractRoleFromRoleField() {
        // Arrange
        AuthInfo authInfo = AuthInfo.builder()
                .claims(java.util.Map.of("role", "ADMIN"))
                .build();

        // Act
        String role = service.extractRole(authInfo);

        // Assert
        assertEquals("ADMIN", role);
    }

    @Test
    void extractRole_ShouldFallbackToRolesField() {
        // Arrange
        AuthInfo authInfo = AuthInfo.builder()
                .claims(java.util.Map.of("roles", "USER"))
                .build();

        // Act
        String role = service.extractRole(authInfo);

        // Assert
        assertEquals("USER", role);
    }

    @Test
    void extractFirstName_ShouldExtractFromFirstNameField() {
        // Arrange
        AuthInfo authInfo = AuthInfo.builder()
                .claims(java.util.Map.of("firstName", "Alice"))
                .build();

        // Act
        String firstName = service.extractFirstName(authInfo);

        // Assert
        assertEquals("Alice", firstName);
    }

    @Test
    void extractFirstName_ShouldFallbackToGivenNameField() {
        // Arrange
        AuthInfo authInfo = AuthInfo.builder()
                .claims(java.util.Map.of("given_name", "Bob"))
                .build();

        // Act
        String firstName = service.extractFirstName(authInfo);

        // Assert
        assertEquals("Bob", firstName);
    }
}
