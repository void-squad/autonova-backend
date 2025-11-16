package com.voidsquad.chatbot.config;

import com.voidsquad.chatbot.util.JsonPathKeyDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration beans for JSON decoding utilities.
 */
@Configuration
public class DecoderConfig {

    @Bean
    public JsonPathKeyDecoder jsonPathKeyDecoder() {
        return new JsonPathKeyDecoder();
    }
}
