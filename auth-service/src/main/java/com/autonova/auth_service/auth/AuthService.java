package com.autonova.auth_service.auth;

import com.autonova.auth_service.security.JwtService;
import com.autonova.auth_service.user.User;
import com.autonova.auth_service.user.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }
        User user = new User();
        user.setEmail(request.email());
        user.setFullName(request.fullName());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userRepository.save(user);

        var claims = new HashMap<String, Object>();
        claims.put("uid", user.getId());
        String token = jwtService.generateToken(user.getEmail(), claims);
        return new AuthResponse(token, new AuthResponse.UserData(user.getId(), user.getEmail(), user.getFullName()));
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (AuthenticationException e) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        User user = userRepository.findByEmail(request.email()).orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        var claims = new HashMap<String, Object>();
        claims.put("uid", user.getId());
        String token = jwtService.generateToken(user.getEmail(), claims);
        return new AuthResponse(token, new AuthResponse.UserData(user.getId(), user.getEmail(), user.getFullName()));
    }
}
