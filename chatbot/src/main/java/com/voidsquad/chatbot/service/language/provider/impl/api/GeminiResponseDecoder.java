package com.voidsquad.chatbot.service.language.provider.impl.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GeminiResponseDecoder {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static DecodedResponse decode(String jsonResponse) {
        try {
            JsonNode root = mapper.readTree(jsonResponse);

            String content = root.path("choices").get(0)
                    .path("message").path("content").asText("");

            int completionTokens = root.path("usage").path("completion_tokens").asInt(0);
            int promptTokens = root.path("usage").path("prompt_tokens").asInt(0);

            return new DecodedResponse(content, completionTokens, promptTokens);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode Gemini response", e);
        }
    }

    public record DecodedResponse(String content, int completionTokens, int promptTokens) {}
}