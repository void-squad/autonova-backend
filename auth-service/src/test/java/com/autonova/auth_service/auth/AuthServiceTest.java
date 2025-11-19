package com.autonova.auth_service.auth;

import com.autonova.auth_service.event.AuthEventPublisher;
import com.autonova.auth_service.security.JwtService;
import com.autonova.auth_service.security.model.RefreshToken;
import com.autonova.auth_service.security.service.RefreshTokenService;
import com.autonova.auth_service.user.model.User;
import com.autonova.auth_service.user.Role;
import com.autonova.auth_service.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private AuthEventPublisher authEventPublisher;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private LoginRequest validLoginRequest;
    private RefreshToken testRefreshToken;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setPassword("$2a$10$hashedPassword");
        testUser.setUserName("testuser");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRole(Role.CUSTOMER);
        testUser.setEnabled(true);

        validLoginRequest = new LoginRequest();
        validLoginRequest.setEmail("test@example.com");
        validLoginRequest.setPassword("password123");

        testRefreshToken = new RefreshToken();
        testRefreshToken.setToken("refresh-token-123");
        testRefreshToken.setUser(testUser);
        testRefreshToken.setExpiryDate(Instant.now().plusSeconds(7 * 24 * 60 * 60));
    }

    @Test
    void login_withValidCredentials_returnsLoginResponse() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "$2a$10$hashedPassword")).thenReturn(true);
        when(jwtService.generateToken(anyLong(), anyString(), anyString(), anyString()))
                .thenReturn("access-token-123");
        when(refreshTokenService.createRefreshToken(1L)).thenReturn(testRefreshToken);

        // When
        LoginResponse response = authService.login(validLoginRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("access-token-123");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token-123");
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getId()).isEqualTo(1L);
        assertThat(response.getUser().getEmail()).isEqualTo("test@example.com");
        assertThat(response.getUser().getRole()).isEqualTo("CUSTOMER");

        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder).matches("password123", "$2a$10$hashedPassword");
        verify(jwtService).generateToken(1L, "test@example.com", "CUSTOMER", "Test");
        verify(refreshTokenService).createRefreshToken(1L);
        verify(authEventPublisher).publishUserLoggedIn(testUser);
    }

    @Test
    void login_withNullEmail_throwsIllegalArgumentException() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail(null);
        request.setPassword("password123");

        // When/Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email is required");

        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void login_withEmptyEmail_throwsIllegalArgumentException() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("   ");
        request.setPassword("password123");

        // When/Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email is required");

        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void login_withNullPassword_throwsIllegalArgumentException() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword(null);

        // When/Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password is required");

        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void login_withEmptyPassword_throwsIllegalArgumentException() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("   ");

        // When/Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password is required");

        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void login_withNonExistentEmail_throwsIllegalArgumentException() {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setEmail("nonexistent@example.com");
        request.setPassword("password123");

        // When/Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email or password");

        verify(userRepository).findByEmail("nonexistent@example.com");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void login_withDisabledAccount_throwsIllegalArgumentException() {
        // Given
        testUser.setEnabled(false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When/Then
        assertThatThrownBy(() -> authService.login(validLoginRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Account is disabled");

        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void login_withWrongPassword_throwsIllegalArgumentException() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", "$2a$10$hashedPassword")).thenReturn(false);

        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongpassword");

        // When/Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email or password");

        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder).matches("wrongpassword", "$2a$10$hashedPassword");
        verify(jwtService, never()).generateToken(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    void login_withEmployeeRole_generatesCorrectTokens() {
        // Given
        testUser.setRole(Role.EMPLOYEE);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "$2a$10$hashedPassword")).thenReturn(true);
        when(jwtService.generateToken(anyLong(), anyString(), anyString(), anyString()))
                .thenReturn("access-token-123");
        when(refreshTokenService.createRefreshToken(1L)).thenReturn(testRefreshToken);

        // When
        LoginResponse response = authService.login(validLoginRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUser().getRole()).isEqualTo("EMPLOYEE");
        verify(jwtService).generateToken(1L, "test@example.com", "EMPLOYEE", "Test");
    }

    @Test
    void login_withAdminRole_generatesCorrectTokens() {
        // Given
        testUser.setRole(Role.ADMIN);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "$2a$10$hashedPassword")).thenReturn(true);
        when(jwtService.generateToken(anyLong(), anyString(), anyString(), anyString()))
                .thenReturn("access-token-123");
        when(refreshTokenService.createRefreshToken(1L)).thenReturn(testRefreshToken);

        // When
        LoginResponse response = authService.login(validLoginRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUser().getRole()).isEqualTo("ADMIN");
        verify(jwtService).generateToken(1L, "test@example.com", "ADMIN", "Test");
    }

    @Test
    void refreshAccessToken_generatesNewAccessToken() {
        // Given
        when(jwtService.generateToken(anyLong(), anyString(), anyString(), anyString()))
                .thenReturn("new-access-token");

        // When
        LoginResponse response = authService.refreshAccessToken(testUser);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("new-access-token");
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getId()).isEqualTo(1L);
        
        verify(jwtService).generateToken(1L, "test@example.com", "CUSTOMER", "Test");
        verify(authEventPublisher).publishUserLoggedIn(testUser);
    }

    @Test
    void login_successfullyPublishesLoginEvent() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "$2a$10$hashedPassword")).thenReturn(true);
        when(jwtService.generateToken(anyLong(), anyString(), anyString(), anyString()))
                .thenReturn("access-token-123");
        when(refreshTokenService.createRefreshToken(1L)).thenReturn(testRefreshToken);

        // When
        authService.login(validLoginRequest);

        // Then
        verify(authEventPublisher).publishUserLoggedIn(testUser);
    }
}
