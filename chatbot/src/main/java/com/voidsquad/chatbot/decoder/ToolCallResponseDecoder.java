package com.voidsquad.chatbot.decoder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voidsquad.chatbot.model.ToolCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ToolCallResponseDecoder {
    private static final Logger log = LoggerFactory.getLogger(ToolCallResponseDecoder.class);

    private final ObjectMapper objectMapper;

    public ToolCallResponseDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Decode the provided AI response text and return a list of ToolCall objects.
     * If decoding fails or no tool_calls key is present, an empty list is returned.
     */
    public List<ToolCall> decode(String text) {
        if (text == null || text.isBlank()) return List.of();

        String cleaned = stripUntilJsonStart(text);
        if (cleaned == null) return List.of();

        try {
            JsonNode root = objectMapper.readTree(cleaned);
            JsonNode calls = root.path("tool_calls");
            if (!calls.isArray()) return List.of();

            List<ToolCall> out = new ArrayList<>();
            for (JsonNode n : calls) {
                try {
                    ToolCall tc = objectMapper.treeToValue(n, ToolCall.class);
                    out.add(tc);
                } catch (JsonProcessingException e) {
                    log.warn("Failed to map tool_call element to ToolCall: {}", e.getMessage());
                }
            }
            return out;
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse tool call JSON: {}", e.getMessage());
            return List.of();
        }
    }

    private String stripUntilJsonStart(String text) {
        int idx = text.indexOf('{');
        if (idx >= 0) return text.substring(idx);
        // maybe the LLM returned a bare array (unlikely for tool_calls but handle)
        idx = text.indexOf('[');
        if (idx >= 0) return text.substring(idx);
        log.debug("No JSON object/array start found in text");
        return null;
    }
}
