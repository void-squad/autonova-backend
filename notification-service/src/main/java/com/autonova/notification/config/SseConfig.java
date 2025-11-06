package com.autonova.notification.config;

import com.autonova.notification.sse.SseHub;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SseConfig {
    @Bean
    public SseHub sseHub() {
        return new SseHub();
    }
}

