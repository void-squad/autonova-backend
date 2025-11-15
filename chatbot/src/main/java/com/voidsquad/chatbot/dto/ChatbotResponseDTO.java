package com.voidsquad.chatbot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * DTO returned by GET /api/v1/chatbot
 * {
 *   "response": "...",
 *   "tokens": { "available": 123, "used": 10 }
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotResponseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonProperty("response")
    private String response;

    @JsonProperty("tokens")
    private Tokens tokens;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Tokens implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        @JsonProperty("available")
        private int available;

        @JsonProperty("used")
        private int used;
    }

}
