package com.autonova.employee_dashboard_service.security;

import java.io.IOException;
import java.util.Collections;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7);
            
            if (jwtService.validateToken(jwt)) {
                String username = jwtService.extractUsername(jwt);
                Long userId = jwtService.extractUserId(jwt);
                String role = jwtService.extractRole(jwt);

                if (username == null || userId == null || role == null || role.isBlank()) {
                    log.warn("Skipping authentication setup due to missing claims (username={}, userId={}, role={})",
                            username, userId, role);
                    filterChain.doFilter(request, response);
                    return;
                }

                String normalizedRole = role.trim();
                if (normalizedRole.toUpperCase().startsWith("ROLE_")) {
                    normalizedRole = normalizedRole.substring(5);
                }
                normalizedRole = normalizedRole.toUpperCase();

                // Create authentication token with role
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + normalizedRole);
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        Collections.singletonList(authority)
                );

                // Set additional details (userId)
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Store userId in request attribute for easy access in controllers
                request.setAttribute("userId", userId);
                request.setAttribute("userRole", normalizedRole);

                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("User authenticated: {} with role: {}", username, normalizedRole);
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
