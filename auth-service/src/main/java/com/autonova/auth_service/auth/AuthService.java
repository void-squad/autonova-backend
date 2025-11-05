package com.autonova.auth_service.auth;

import com.autonova.auth_service.event.AuthEventPublisher;
import com.autonova.auth_service.security.JwtService;
import com.autonova.auth_service.user.model.User;
import com.autonova.auth_service.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthEventPublisher authEventPublisher;

    public AuthService(UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthEventPublisher authEventPublisher) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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

        // Generate JWT token
        String token = jwtService.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
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

        // Return login response
        return new LoginResponse(token, userInfo);
    }
}
