package com.autonova.payments_billing_service.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final SecretKey secretKey;

    public JwtAuthenticationFilter(AuthProperties properties) {
        Objects.requireNonNull(properties, "properties");
        byte[] keyBytes = Decoders.BASE64.decode(properties.getHs256Secret());
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
            || path.startsWith("/actuator")
            || path.startsWith("/v3/api-docs")
            || path.startsWith("/swagger-ui")
            || path.startsWith("/webhooks/stripe");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.isBlank() || !authorization.startsWith("Bearer ")) {
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring("Bearer ".length());
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

            String email = Optional.ofNullable(claims.getSubject())
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .orElseThrow(() -> new JwtException("Token missing subject"));

            Long userId = extractUserId(claims);
            Set<String> roles = extractRoles(claims);
            if (roles.isEmpty()) {
                throw new JwtException("Token contains no role claim");
            }

            AuthenticatedUser principal = new AuthenticatedUser(userId, email, roles);
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.asAuthorities()
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (IllegalArgumentException | JwtException ex) {
            log.warn("JWT validation failed: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
            response.setHeader("WWW-Authenticate", "Bearer error=\"invalid_token\"");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    private Long extractUserId(Claims claims) {
        Object value = claims.get("userId");
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String str && !str.isBlank()) {
            try {
                return Long.parseLong(str.trim());
            } catch (NumberFormatException ex) {
                throw new JwtException("Token userId claim is not a number", ex);
            }
        }
        throw new JwtException("Token missing userId claim");
    }

    private Set<String> extractRoles(Claims claims) {
        Object roleClaim = claims.get("role");
        if (roleClaim == null) {
            return Set.of();
        }

        String role = roleClaim.toString().trim();
        if (role.isEmpty()) {
            return Set.of();
        }

        Set<String> roles = new HashSet<>();
        roles.add(role);
        return roles;
    }
}
