package com.voidsquad.chatbot.service.language.provider.impl.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

public class AiRequestBuilder {
    private String model;
    private final List<Map<String, String>> messages = new ArrayList<>();

    public static AiRequestBuilder create() {
        return new AiRequestBuilder();
    }

    public AiRequestBuilder model(String model) {
        this.model = model;
        return this;
    }

    public AiRequestBuilder message(String role, String content) {
        Map<String, String> msg = new HashMap<>();
        msg.put("role", role);
        msg.put("content", content);
        messages.add(msg);
        return this;
    }

    public String buildJson() {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("model", model);
            request.put("messages", messages);
            return new ObjectMapper().writeValueAsString(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build AI request JSON", e);
        }
    }

    public Map<String, Object> buildMap() {
        Map<String, Object> request = new HashMap<>();
        request.put("model", model);
        request.put("messages", messages);
        return request;
    }
}
