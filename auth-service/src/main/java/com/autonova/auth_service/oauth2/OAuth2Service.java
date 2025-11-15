package com.autonova.auth_service.oauth2;

import com.autonova.auth_service.auth.LoginResponse;
import com.autonova.auth_service.event.AuthEventPublisher;
import com.autonova.auth_service.security.JwtService;
import com.autonova.auth_service.security.model.RefreshToken;
import com.autonova.auth_service.security.service.RefreshTokenService;
import com.autonova.auth_service.user.Role;
import com.autonova.auth_service.user.model.User;
import com.autonova.auth_service.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * OAuth2 Authentication Service
 * Handles Google OAuth2 login
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2Service {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthEventPublisher authEventPublisher;

    /**
     * Process OAuth2 login
     * - If user exists: return JWT
     * - If new user: create account and return JWT
     */
    @Transactional
    public LoginResponse processOAuth2Login(OAuth2User oauth2User) {
        Map<String, Object> attributes = oauth2User.getAttributes();
        
        // Extract user info from Google
        OAuth2UserInfo userInfo = OAuth2UserInfo.fromGoogle(attributes);
        
        log.info("OAuth2 Login attempt for email: {}", userInfo.getEmail());
        
        // Find or create user
        User user = userRepository.findByEmail(userInfo.getEmail())
                .orElseGet(() -> createNewOAuth2User(userInfo));
        
        // Check if user is enabled
        if (!user.isEnabled()) {
            throw new IllegalArgumentException("Account is disabled");
        }
        
        // Generate JWT access token (1 hour)
        String accessToken = jwtService.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.getFirstName()
        );
        
        // Generate refresh token (7 days)
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());
        
        // Create user info response
        LoginResponse.UserInfo userInfoResponse = new LoginResponse.UserInfo(
                user.getId(),
                user.getUserName(),
                user.getEmail(),
                user.getRole().name()
        );
        
        log.info("✅ OAuth2 Login successful for user: {}", user.getEmail());
        
        // Notify downstream services so their customer records stay in sync
        authEventPublisher.publishUserLoggedIn(user);

        // Return login response with both tokens
        return new LoginResponse(accessToken, refreshToken.getToken(), userInfoResponse);
    }

    /**
     * Create new user from OAuth2 provider (Google)
     */
    private User createNewOAuth2User(OAuth2UserInfo userInfo) {
        log.info("Creating new user from OAuth2 provider: {}", userInfo.getEmail());
        
        User newUser = new User();
        newUser.setEmail(userInfo.getEmail());
        newUser.setUserName(userInfo.getName() != null ? userInfo.getName() : userInfo.getEmail());
        
        // OAuth2 users don't have passwords (they authenticate via Google)
        // Set a random password that can't be used for normal login
        newUser.setPassword("OAUTH2_USER_NO_PASSWORD");
        
        // Default role for OAuth2 users
        newUser.setRole(Role.CUSTOMER);
        newUser.setEnabled(true);
        
        // Optional: Store additional info
        // Could add fields like: profilePicture, oauthProvider, oauthProviderId
        
        User savedUser = userRepository.save(newUser);
        log.info("✅ New OAuth2 user created: {}", savedUser.getEmail());
        
        return savedUser;
    }
}
