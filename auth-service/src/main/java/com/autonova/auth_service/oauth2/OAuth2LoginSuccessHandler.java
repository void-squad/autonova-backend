package com.autonova.auth_service.oauth2;

import com.autonova.auth_service.auth.LoginResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * OAuth2 Login Success Handler
 * Handles successful Google OAuth2 authentication
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final OAuth2Service oauth2Service;
    private final ObjectMapper objectMapper;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {
        
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        
        try {
            // Process OAuth2 login and get JWT tokens
            LoginResponse loginResponse = oauth2Service.processOAuth2Login(oauth2User);
            
            log.info("✅ OAuth2 authentication successful, redirecting to frontend");
            
            // Redirect to frontend with tokens as query parameters
            // Frontend will extract and store these tokens
            String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/callback")
                    .queryParam("token", loginResponse.getToken())
                    .queryParam("refreshToken", loginResponse.getRefreshToken())
                    .queryParam("userId", loginResponse.getUser().getId())
                    .queryParam("email", loginResponse.getUser().getEmail())
                    .queryParam("role", loginResponse.getUser().getRole())
                    .build()
                    .toUriString();
            
            response.sendRedirect(redirectUrl);
            
        } catch (Exception e) {
            log.error("❌ OAuth2 authentication failed", e);
            
            // Redirect to frontend with error
            String errorUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/callback")
                    .queryParam("error", URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8))
                    .build()
                    .toUriString();
            
            response.sendRedirect(errorUrl);
        }
    }
}
