package com.voidsquad.chatbot.service.promptmanager.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voidsquad.chatbot.service.promptmanager.PromptStrategy;
import com.voidsquad.chatbot.service.promptmanager.core.OutputFormat;
import com.voidsquad.chatbot.service.promptmanager.core.ProcessingResult;
import com.voidsquad.chatbot.service.promptmanager.core.ProcessingType;
import com.voidsquad.chatbot.service.promptmanager.core.PromptConfig;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SimpleChatStrategy implements PromptStrategy {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public PromptConfig getConfig() {
        return new PromptConfig(
                """
You are a concise quick-reply assistant. Decide if the user's request
can be answered immediately using only the provided short context and general knowledge.
If so, produce a short helpful answer. If not, indicate that a longer workflow is required.

RESPONSE RULES:
- Output MUST be valid JSON with the exact structure: {"isSimple": true|false, "data": "..."}
- If a simple factual answer is possible (one or two sentences), set isSimple to true and put the answer in data.
- If additional tool calls, retrievals or multi-step reasoning are required, set isSimple to false and keep data brief (optional reasoning).
                """,
                """
User Request: {userPrompt}

Relevant Static Context: {context}

Decide whether a simple response suffices. Output only the required JSON.
                """,
                OutputFormat.JSON,
                0.0,
                300
        );
    }

    @Override
    public String buildUserPrompt(String userInput, String context, Map<String, Object> params) {
        return getConfig().userPromptTemplate()
                .replace("{userPrompt}", userInput)
                .replace("{context}", context != null ? context : "");
    }

    @Override
    public String buildSystemPrompt(String systemContext) {
        return getConfig().systemPrompt();
    }

    @Override
    public ProcessingResult postProcess(String llmOutput) {
        try {
            JsonNode node = objectMapper.readTree(llmOutput);
            boolean isSimple = node.has("isSimple") && node.get("isSimple").asBoolean(false);
            String data = node.has("data") ? node.get("data").asText("") : "";

            return new ProcessingResult(
                    llmOutput,
                    OutputFormat.JSON,
                    ProcessingType.SIMPLE_CHAT,
                    Map.of("isSimple", isSimple, "data", data)
            );
        } catch (Exception e) {
            // If parsing fails, treat as non-simple to allow workflow
            return new ProcessingResult(
                    llmOutput,
                    OutputFormat.JSON,
                    ProcessingType.SIMPLE_CHAT,
                    Map.of("isSimple", false, "parse_error", e.getMessage())
            );
        }
    }
}
