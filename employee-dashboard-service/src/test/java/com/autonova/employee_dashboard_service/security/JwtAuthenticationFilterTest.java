package com.autonova.employee_dashboard_service.security;

import com.autonova.employee_dashboard_service.security.JwtAuthenticationFilter;
import com.autonova.employee_dashboard_service.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Unit Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should skip authentication when no Authorization header present")
    void shouldSkipAuthenticationWhenNoHeader() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn(null);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).validateToken(any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Should skip authentication when Authorization header does not start with Bearer")
    void shouldSkipAuthenticationWhenNotBearerToken() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Basic sometoken");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).validateToken(any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Should authenticate user when valid JWT token provided")
    void shouldAuthenticateUserWhenValidToken() throws ServletException, IOException {
        // Given
        String token = "valid.jwt.token";
        String authHeader = "Bearer " + token;
        String username = "test@autonova.com";
        Long userId = 123L;
        String role = "EMPLOYEE";

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtService.validateToken(token)).thenReturn(true);
        when(jwtService.extractUsername(token)).thenReturn(username);
        when(jwtService.extractUserId(token)).thenReturn(userId);
        when(jwtService.extractRole(token)).thenReturn(role);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtService).validateToken(token);
        verify(jwtService).extractUsername(token);
        verify(jwtService).extractUserId(token);
        verify(jwtService).extractRole(token);
        verify(request).setAttribute("userId", userId);
        verify(request).setAttribute("userRole", role);
        verify(filterChain).doFilter(request, response);
        
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo(username);
    }

    @Test
    @DisplayName("Should not authenticate when token validation fails")
    void shouldNotAuthenticateWhenTokenInvalid() throws ServletException, IOException {
        // Given
        String token = "invalid.jwt.token";
        String authHeader = "Bearer " + token;

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtService.validateToken(token)).thenReturn(false);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtService).validateToken(token);
        verify(jwtService, never()).extractUsername(any());
        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Should continue filter chain even when JWT processing throws exception")
    void shouldContinueFilterChainWhenExceptionOccurs() throws ServletException, IOException {
        // Given
        String token = "malformed.token";
        String authHeader = "Bearer " + token;

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtService.validateToken(token)).thenThrow(new RuntimeException("Token parsing error"));

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Should set user attributes in request for valid token")
    void shouldSetUserAttributesInRequest() throws ServletException, IOException {
        // Given
        String token = "valid.jwt.token";
        String authHeader = "Bearer " + token;
        Long userId = 456L;
        String role = "MANAGER";

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtService.validateToken(token)).thenReturn(true);
        when(jwtService.extractUsername(token)).thenReturn("manager@autonova.com");
        when(jwtService.extractUserId(token)).thenReturn(userId);
        when(jwtService.extractRole(token)).thenReturn(role);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(request).setAttribute("userId", userId);
        verify(request).setAttribute("userRole", role);
    }

    @Test
    @DisplayName("Should grant correct authority based on role")
    void shouldGrantCorrectAuthorityBasedOnRole() throws ServletException, IOException {
        // Given
        String token = "valid.jwt.token";
        String authHeader = "Bearer " + token;
        String role = "EMPLOYEE";

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtService.validateToken(token)).thenReturn(true);
        when(jwtService.extractUsername(token)).thenReturn("employee@autonova.com");
        when(jwtService.extractUserId(token)).thenReturn(789L);
        when(jwtService.extractRole(token)).thenReturn(role);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .contains("ROLE_" + role);
    }
}
