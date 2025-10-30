package com.autonova.payments_billing_service.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final AuthProperties properties;
    private final SecretKey secretKey;

    public JwtAuthenticationFilter(AuthProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.secretKey = Keys.hmacShaKeyFor(properties.getHs256Secret().getBytes(StandardCharsets.UTF_8));
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
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            SecurityContextHolder.clearContext();
            response.setHeader("WWW-Authenticate", "Bearer");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String token = authorization.substring("Bearer ".length());
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .requireIssuer(properties.getIssuer())
                .requireAudience(properties.getAudience())
                .build()
                .parseClaimsJws(token)
                .getBody();

            String subject = Optional.ofNullable(claims.getSubject())
                .orElseThrow(() -> new JwtException("Token missing subject"));

            UUID userId = UUID.fromString(subject);
            Set<String> roles = extractRoles(claims);
            if (roles.isEmpty()) {
                throw new JwtException("Token contains no roles");
            }

            AuthenticatedUser principal = new AuthenticatedUser(userId, roles);
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

    private Set<String> extractRoles(Claims claims) {
        Object rolesClaim = claims.get("roles");
        if (rolesClaim == null) {
            return Set.of();
        }

        Set<String> roles = new HashSet<>();
        if (rolesClaim instanceof Collection<?> collection) {
            for (Object value : collection) {
                if (value != null) {
                    roles.add(value.toString());
                }
            }
        } else if (rolesClaim instanceof String str) {
            if (!str.isBlank()) {
                List.of(str.split(",")).forEach(role -> roles.add(role.trim()));
            }
        }

        return roles;
    }
}
