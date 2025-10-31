package com.autonova.payments_billing_service.config;

import com.autonova.payments_billing_service.auth.AuthProperties;
import com.autonova.payments_billing_service.auth.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter(AuthProperties properties) {
        return new JwtAuthenticationFilter(properties);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(
                    "/actuator/health",
                    "/actuator/info",
                    "/v3/api-docs",
                    "/v3/api-docs/**",
                    "/v3/api-docs.yaml",
                    "/v3/api-docs.yml",
                    "/swagger-ui/**",
                    "/webhooks/stripe"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
