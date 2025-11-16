package com.autonova.auth_service.auth;

import com.autonova.auth_service.event.AuthEventPublisher;
import com.autonova.auth_service.security.JwtService;
import com.autonova.auth_service.security.model.RefreshToken;
import com.autonova.auth_service.security.service.RefreshTokenService;
import com.autonova.auth_service.user.model.User;
import com.autonova.auth_service.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthEventPublisher authEventPublisher;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            AuthEventPublisher authEventPublisher) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.authEventPublisher = authEventPublisher;
    }

    public LoginResponse login(LoginRequest request) {
        // Validate input
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }

        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        // Check if user is enabled
        if (!user.isEnabled()) {
            throw new IllegalArgumentException("Account is disabled");
        }

        // Validate password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
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

        // Create user info
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                user.getId(),
                user.getUserName(),
                user.getEmail(),
                user.getRole().name()
        );

        // Emit auth event so downstream services can sync the customer profile immediately
        authEventPublisher.publishUserLoggedIn(user);

        // Return login response with both tokens
        return new LoginResponse(accessToken, refreshToken.getToken(), userInfo);
    }

    /**
     * Refresh access token using refresh token
     */
    public LoginResponse refreshAccessToken(User user) {
        // Generate new JWT access token
        String accessToken = jwtService.generateToken(
                user.getId(),
                user.getEmail(),
        user.getRole().name(),
        user.getFirstName()
        );

    // Emit login event for downstream services
    authEventPublisher.publishUserLoggedIn(user);

    // Create user info
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                user.getId(),
                user.getUserName(),
                user.getEmail(),
                user.getRole().name()
        );

        // Return new access token (refresh token stays the same)
        return new LoginResponse(accessToken, null, userInfo);
    }
}