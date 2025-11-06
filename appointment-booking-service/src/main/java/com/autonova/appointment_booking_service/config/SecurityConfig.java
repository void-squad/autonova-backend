package com.autonova.appointment_booking_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Value("${app.security.enabled:true}")
    private boolean securityEnabled;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, ObjectProvider<JwtDecoder> jwtDecoderProvider) throws Exception {
        http.csrf().disable();

        if (!securityEnabled) {
            // Permit all requests when security is disabled (testing)
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }

        // Default secured behavior
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/appointments/**").authenticated()
                        .anyRequest().permitAll()
                );

        // Only configure the OAuth2 resource server if a JwtDecoder bean is present.
        // This avoids startup failures when JWT configuration (jwk-set-uri etc.) is not provided.
        if (jwtDecoderProvider.getIfAvailable() != null) {
            http.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults())); // assumes JWTs
        }

        return http.build();
    }
}
