package com.voidsquad.chatbot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
public class ActuatorSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
          .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/health", "/actuator/info", "/actuator/metrics/**").permitAll()
            .anyRequest().permitAll() //TODO: change later
          )
          .csrf(csrf -> csrf.disable());
        return http.build();
    }
}