package com.autonova.auth_service.security.service;

import com.autonova.auth_service.security.model.RefreshToken;
import com.autonova.auth_service.security.repository.RefreshTokenRepository;
import com.autonova.auth_service.user.model.User;
import com.autonova.auth_service.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService Unit Tests")
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User testUser;
    private RefreshToken testRefreshToken;

    @BeforeEach
    void setUp() {
        // Set refresh token expiration to 7 days
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenDurationMs", 604800000L);

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");

        testRefreshToken = new RefreshToken(
                "test-refresh-token",
                testUser,
                Instant.now().plusMillis(604800000L)
        );
        testRefreshToken.setId(1L);
    }

    @Test
    @DisplayName("Should create refresh token for user")
    void createRefreshToken_WithValidUser_ShouldCreateToken() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        doNothing().when(refreshTokenRepository).deleteByUser(testUser);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(testRefreshToken);

        // When
        RefreshToken result = refreshTokenService.createRefreshToken(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUser()).isEqualTo(testUser);
        verify(userRepository).findById(1L);
        verify(refreshTokenRepository).deleteByUser(testUser);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should throw exception when user not found for refresh token creation")
    void createRefreshToken_WithNonExistentUser_ShouldThrowException() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> refreshTokenService.createRefreshToken(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");

        verify(userRepository).findById(999L);
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should delete existing tokens when creating new one")
    void createRefreshToken_ShouldDeleteExistingTokens() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        doNothing().when(refreshTokenRepository).deleteByUser(testUser);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(testRefreshToken);

        // When
        refreshTokenService.createRefreshToken(1L);

        // Then
        verify(refreshTokenRepository).deleteByUser(testUser);
    }

    @Test
    @DisplayName("Should find refresh token by token string")
    void findByToken_WithValidToken_ShouldReturnToken() {
        // Given
        String tokenString = "test-refresh-token";
        when(refreshTokenRepository.findByToken(tokenString)).thenReturn(Optional.of(testRefreshToken));

        // When
        RefreshToken result = refreshTokenService.findByToken(tokenString);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getToken()).isEqualTo(tokenString);
        verify(refreshTokenRepository).findByToken(tokenString);
    }

    @Test
    @DisplayName("Should throw exception when token not found")
    void findByToken_WithNonExistentToken_ShouldThrowException() {
        // Given
        String tokenString = "non-existent-token";
        when(refreshTokenRepository.findByToken(tokenString)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> refreshTokenService.findByToken(tokenString))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Refresh token not found");

        verify(refreshTokenRepository).findByToken(tokenString);
    }

    @Test
    @DisplayName("Should verify non-expired token successfully")
    void verifyExpiration_WithValidToken_ShouldReturnToken() {
        // Given
        RefreshToken validToken = new RefreshToken(
                "valid-token",
                testUser,
                Instant.now().plusMillis(3600000L) // 1 hour from now
        );

        // When
        RefreshToken result = refreshTokenService.verifyExpiration(validToken);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isExpired()).isFalse();
        verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should delete expired token and throw exception")
    void verifyExpiration_WithExpiredToken_ShouldThrowException() {
        // Given
        RefreshToken expiredToken = new RefreshToken(
                "expired-token",
                testUser,
                Instant.now().minusMillis(3600000L) // 1 hour ago
        );
        doNothing().when(refreshTokenRepository).delete(expiredToken);

        // When & Then
        assertThatThrownBy(() -> refreshTokenService.verifyExpiration(expiredToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Refresh token has expired");

        verify(refreshTokenRepository).delete(expiredToken);
    }

    @Test
    @DisplayName("Should revoke token successfully")
    void revokeToken_WithValidToken_ShouldRevokeToken() {
        // Given
        String tokenString = "test-refresh-token";
        when(refreshTokenRepository.findByToken(tokenString)).thenReturn(Optional.of(testRefreshToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(testRefreshToken);

        // When
        refreshTokenService.revokeToken(tokenString);

        // Then
        verify(refreshTokenRepository).findByToken(tokenString);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should throw exception when revoking non-existent token")
    void revokeToken_WithNonExistentToken_ShouldThrowException() {
        // Given
        String tokenString = "non-existent-token";
        when(refreshTokenRepository.findByToken(tokenString)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> refreshTokenService.revokeToken(tokenString))
                .isInstanceOf(IllegalArgumentException.class);

        verify(refreshTokenRepository).findByToken(tokenString);
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should revoke all user tokens successfully")
    void revokeAllUserTokens_WithValidUser_ShouldDeleteAllTokens() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        doNothing().when(refreshTokenRepository).deleteByUser(testUser);

        // When
        refreshTokenService.revokeAllUserTokens(1L);

        // Then
        verify(userRepository).findById(1L);
        verify(refreshTokenRepository).deleteByUser(testUser);
    }

    @Test
    @DisplayName("Should throw exception when revoking tokens for non-existent user")
    void revokeAllUserTokens_WithNonExistentUser_ShouldThrowException() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> refreshTokenService.revokeAllUserTokens(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");

        verify(userRepository).findById(999L);
        verify(refreshTokenRepository, never()).deleteByUser(any(User.class));
    }

    @Test
    @DisplayName("Should delete expired tokens")
    void deleteExpiredTokens_ShouldCallRepository() {
        // Given
        doNothing().when(refreshTokenRepository).deleteExpiredTokens(any(Instant.class));

        // When
        refreshTokenService.deleteExpiredTokens();

        // Then
        verify(refreshTokenRepository).deleteExpiredTokens(any(Instant.class));
    }

    @Test
    @DisplayName("Should delete revoked tokens")
    void deleteRevokedTokens_ShouldCallRepository() {
        // Given
        doNothing().when(refreshTokenRepository).deleteRevokedTokens();

        // When
        refreshTokenService.deleteRevokedTokens();

        // Then
        verify(refreshTokenRepository).deleteRevokedTokens();
    }

    @Test
    @DisplayName("Should create token with correct expiry date")
    void createRefreshToken_ShouldSetCorrectExpiryDate() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        doNothing().when(refreshTokenRepository).deleteByUser(testUser);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken savedToken = invocation.getArgument(0);
            assertThat(savedToken.getExpiryDate()).isAfter(Instant.now());
            return savedToken;
        });

        // When
        refreshTokenService.createRefreshToken(1L);

        // Then
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }
}
