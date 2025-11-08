package com.autonova.auth_service.auth;

import com.autonova.auth_service.security.model.RefreshToken;
import com.autonova.auth_service.security.service.PasswordResetService;
import com.autonova.auth_service.security.service.RefreshTokenService;
import com.autonova.auth_service.user.model.User;
import com.autonova.auth_service.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Auth Controller Unit Tests")
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private PasswordResetService passwordResetService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthController authController;

    private User testUser;
    private LoginRequest loginRequest;
    private LoginResponse loginResponse;
    private RefreshToken refreshToken;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setRole(Role.CUSTOMER);
        testUser.setUserName("Test User");
        testUser.setEnabled(true);

        // Setup login request
        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("Password123!");

        // Setup login response
        loginResponse = new LoginResponse();
        loginResponse.setToken("access-token-jwt");
        loginResponse.setRefreshToken("refresh-token-uuid");
        loginResponse.setType("Bearer");
        loginResponse.setUser(new LoginResponse.UserInfo(
                testUser.getId(),
                testUser.getUserName(),
                testUser.getEmail(),
                testUser.getRole().name()
        ));

        // Setup refresh token
        refreshToken = new RefreshToken();
        refreshToken.setId(1L);
        refreshToken.setToken("refresh-token-uuid");
        refreshToken.setUser(testUser);
        refreshToken.setExpiryDate(Instant.now().plusSeconds(604800));
        refreshToken.setRevoked(false);
    }

    @Test
    @DisplayName("Should successfully login with valid credentials")
    void testLogin_WithValidCredentials_ShouldReturnTokens() {
        // Given
        when(authService.login(any(LoginRequest.class))).thenReturn(loginResponse);

        // When
        ResponseEntity<?> response = authController.login(loginRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(LoginResponse.class);
        LoginResponse body = (LoginResponse) response.getBody();
        assertThat(body.getToken()).isEqualTo("access-token-jwt");
        assertThat(body.getRefreshToken()).isEqualTo("refresh-token-uuid");
        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("Should return bad request when login fails with invalid credentials")
    void testLogin_WithInvalidCredentials_ShouldReturnBadRequest() {
        // Given
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new IllegalArgumentException("Invalid credentials"));

        // When
        ResponseEntity<?> response = authController.login(loginRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertThat(body).containsKey("error");
        assertThat(body.get("error")).isEqualTo("Invalid credentials");
    }

    @Test
    @DisplayName("Should send password reset email with valid email")
    void testForgotPassword_WithValidEmail_ShouldReturnSuccess() {
        // Given
        Map<String, String> request = new HashMap<>();
        request.put("email", "test@example.com");
        when(passwordResetService.generateResetToken("test@example.com"))
                .thenReturn("reset-token-uuid");

        // When
        ResponseEntity<?> response = authController.forgotPassword(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("success")).isEqualTo(true);
        assertThat(body.get("message")).asString().contains("Password reset link");
        verify(passwordResetService, times(1)).generateResetToken("test@example.com");
    }

    @Test
    @DisplayName("Should return bad request when email is missing in forgot password")
    void testForgotPassword_WithMissingEmail_ShouldReturnBadRequest() {
        // Given
        Map<String, String> request = new HashMap<>();

        // When
        ResponseEntity<?> response = authController.forgotPassword(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertThat(body.get("error")).isEqualTo("Email address is required");
    }

    @Test
    @DisplayName("Should successfully reset password with valid token")
    void testResetPassword_WithValidToken_ShouldReturnSuccess() {
        // Given
        Map<String, String> request = new HashMap<>();
        request.put("token", "reset-token-uuid");
        request.put("newPassword", "NewPassword123!");
        doNothing().when(passwordResetService).resetPassword(anyString(), anyString());

        // When
        ResponseEntity<?> response = authController.resetPassword(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertThat(body.get("message")).contains("successfully");
        verify(passwordResetService, times(1)).resetPassword("reset-token-uuid", "NewPassword123!");
    }

    @Test
    @DisplayName("Should return bad request when reset token is expired")
    void testResetPassword_WithExpiredToken_ShouldReturnBadRequest() {
        // Given
        Map<String, String> request = new HashMap<>();
        request.put("token", "expired-token");
        request.put("newPassword", "NewPassword123!");
        doThrow(new IllegalArgumentException("Token has expired"))
                .when(passwordResetService).resetPassword(anyString(), anyString());

        // When
        ResponseEntity<?> response = authController.resetPassword(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertThat(body.get("error")).isEqualTo("Token has expired");
    }

    @Test
    @DisplayName("Should successfully refresh access token with valid refresh token")
    void testRefreshToken_WithValidToken_ShouldReturnNewAccessToken() {
        // Given
        Map<String, String> request = new HashMap<>();
        request.put("refreshToken", "refresh-token-uuid");
        when(refreshTokenService.findByToken("refresh-token-uuid")).thenReturn(refreshToken);
        when(refreshTokenService.verifyExpiration(refreshToken)).thenReturn(refreshToken);
        when(authService.refreshAccessToken(testUser)).thenReturn(loginResponse);

        // When
        ResponseEntity<?> response = authController.refreshToken(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(LoginResponse.class);
        LoginResponse body = (LoginResponse) response.getBody();
        assertThat(body.getToken()).isEqualTo("access-token-jwt");
        verify(refreshTokenService, times(1)).findByToken("refresh-token-uuid");
        verify(authService, times(1)).refreshAccessToken(testUser);
    }

    @Test
    @DisplayName("Should successfully logout and revoke refresh token")
    void testLogout_WithValidToken_ShouldRevokeToken() {
        // Given
        Map<String, String> request = new HashMap<>();
        request.put("refreshToken", "refresh-token-uuid");
        doNothing().when(refreshTokenService).revokeToken("refresh-token-uuid");

        // When
        ResponseEntity<?> response = authController.logout(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertThat(body.get("message")).contains("Logged out successfully");
        verify(refreshTokenService, times(1)).revokeToken("refresh-token-uuid");
    }
}
