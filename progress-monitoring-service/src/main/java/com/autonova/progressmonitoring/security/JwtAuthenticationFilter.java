package com.autonova.progressmonitoring.security;
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
        log.debug("Processing request: {} {}", request.getMethod(), request.getRequestURI());
        log.debug("Authorization header: {}", authHeader != null ? "present" : "missing");
        
        // Prefer Authorization header (Bearer). If missing, allow `access_token` query param
        // This is necessary for browser EventSource which cannot set custom headers.
        // WARNING: passing JWTs in URLs can expose tokens in logs/history; prefer cookie-based auth if possible.
        String jwt = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            log.debug("JWT token extracted from Authorization header");
        } else {
            // fallback: check request parameter `access_token` (used by some SSE clients)
            final String param = request.getParameter("access_token");
            if (param != null && !param.isEmpty()) {
                jwt = param;
                log.debug("JWT token extracted from access_token query parameter");
            }
        }

        if (jwt == null) {
            log.debug("No JWT token found in request, proceeding without authentication");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (jwtService.validateToken(jwt)) {
                String username = jwtService.extractUsername(jwt);
                Long userId = jwtService.extractUserId(jwt);
                String role = jwtService.extractRole(jwt);
                log.debug("JWT validated - userId: {}, role: {}", userId, role);
                
                // Create authentication token with role
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        Collections.singletonList(authority)
                );
                // Set additional details (userId)
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                // Store userId in request attribute for easy access in controllers
                request.setAttribute("userId", userId);
                request.setAttribute("userRole", role);
                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("User authenticated: {} with role: {}", username, role);
            } else {
                log.warn("JWT token validation failed - token may be expired or invalid");
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage(), e);
        }
        filterChain.doFilter(request, response);
    }
}
