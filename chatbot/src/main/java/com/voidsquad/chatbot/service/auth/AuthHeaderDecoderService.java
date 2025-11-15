package com.voidsquad.chatbot.service.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Service that decodes Authorization header values (supports Bearer JWT) and extracts payload claims.
 *
 * This service returns an internal {@link AuthInfo} model. Use a mapper to convert to
 * {@code AuthInfoDTO} before returning to callers if you need a DTO representation.
 */
@Service
public class AuthHeaderDecoderService {

    private static final Logger log = LoggerFactory.getLogger(AuthHeaderDecoderService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthInfo decode(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return AuthInfo.builder()
                    .valid(false)
                    .error("Authorization header is missing")
                    .build();
        }

        String[] parts = authorizationHeader.split(" ", 2);
        String scheme = parts.length > 0 ? parts[0] : null;
        String token = parts.length > 1 ? parts[1] : null;

        if (scheme == null || token == null) {
            return AuthInfo.builder()
                    .scheme(scheme)
                    .valid(false)
                    .error("Invalid Authorization header format")
                    .build();
        }

        if (!"bearer".equalsIgnoreCase(scheme)) {
            return AuthInfo.builder()
                    .scheme(scheme)
                    .valid(false)
                    .error("Unsupported scheme: " + scheme)
                    .build();
        }

        try {
            // Parse JWT payload (header.payload.signature)
            String[] tokenParts = token.split("\\.");
            if (tokenParts.length < 2) {
                return AuthInfo.builder()
                        .scheme(scheme)
                        .valid(false)
                        .error("Token does not appear to be a JWT (missing parts)")
                        .build();
            }

            String payload = tokenParts[1];
            byte[] decoded = Base64.getUrlDecoder().decode(padBase64(payload));
            String json = new String(decoded, StandardCharsets.UTF_8);

            @SuppressWarnings("unchecked")
            Map<String, Object> claims = objectMapper.readValue(json, Map.class);

            // extract common fields
            Long userId = null;
            Object userIdObj = claims.get("userId");
            if (userIdObj instanceof Number) userId = ((Number) userIdObj).longValue();
            else if (userIdObj instanceof String) {
                try { userId = Long.parseLong(((String) userIdObj).trim()); } catch (NumberFormatException ignored) {}
            }

            String email = claims.get("email") != null ? claims.get("email").toString() : null;
            Object roleObj = claims.get("role");
            if (roleObj == null) roleObj = claims.get("roles");
            String role = roleObj != null ? roleObj.toString() : null;
            String firstName = claims.get("firstName") != null ? claims.get("firstName").toString() :
                    (claims.get("given_name") != null ? claims.get("given_name").toString() : null);

            return AuthInfo.builder()
                    .scheme(scheme)
                    .userId(userId)
                    .email(email)
                    .role(role)
                    .firstName(firstName)
                    .claims(claims)
                    .valid(true)
                    .build();

        } catch (Exception e) {
            log.warn("Failed to decode Authorization header: {}", e.getMessage());
            return AuthInfo.builder()
                    .scheme(authorizationHeader == null ? null : authorizationHeader.split(" ")[0])
                    .valid(false)
                    .error("Failed to decode token payload: " + e.getMessage())
                    .build();
        }
    }

    // Convenience extractors for common claims.
    public Long extractUserId(AuthInfo info) {
        if (info == null || info.getClaims() == null) return null;
        Object value = info.getClaims().get("userId");
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            String str = ((String) value).trim();
            if (str.isEmpty()) return null;
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException ex) {
                log.warn("userId claim is not a number: {}", str);
                return null;
            }
        }
        return null;
    }

    public String extractEmail(AuthInfo info) {
        if (info == null || info.getClaims() == null) return null;
        Object v = info.getClaims().get("email");
        return v != null ? v.toString() : null;
    }

    public String extractRole(AuthInfo info) {
        if (info == null || info.getClaims() == null) return null;
        Object v = info.getClaims().get("role");
        if (v == null) v = info.getClaims().get("roles");
        return v != null ? v.toString() : null;
    }

    public String extractFirstName(AuthInfo info) {
        if (info == null || info.getClaims() == null) return null;
        Object v = info.getClaims().get("firstName");
        if (v == null) v = info.getClaims().get("given_name");
        return v != null ? v.toString() : null;
    }

    // Some JWT libraries omit padding; ensure length is multiple of 4 for Base64 decoder
    private String padBase64(String b64) {
        int rem = b64.length() % 4;
        if (rem == 0) return b64;
        return b64 + "".repeat(4 - rem);
    }

}
