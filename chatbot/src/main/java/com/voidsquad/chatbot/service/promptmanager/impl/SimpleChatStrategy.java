package com.voidsquad.chatbot.service.promptmanager.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voidsquad.chatbot.service.language.LanguageProcessor;
import com.voidsquad.chatbot.service.promptmanager.PromptStrategy;
import com.voidsquad.chatbot.service.promptmanager.core.OutputFormat;
import com.voidsquad.chatbot.service.promptmanager.core.ProcessingResult;
import com.voidsquad.chatbot.service.promptmanager.core.ProcessingType;
import com.voidsquad.chatbot.service.promptmanager.core.PromptConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class SimpleChatStrategy implements PromptStrategy {

    private static final Logger log = LogManager.getLogger(LanguageProcessor.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ResourcePatternResolver resourcePatternResolver;

    public SimpleChatStrategy(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    @Override
    public PromptConfig getConfig() {
        return new PromptConfig(
                """
You are a concise AI assistant. Always respond strictly in JSON format only.

RESPONSE RULES:
- Output MUST be valid JSON with exactly this structure: {"isSimple": true|false, "data": "..."}
- No extra text, no quotes, no explanations outside the JSON object.
- If any answer can be given, set the isSimple to true and provide the answer in "data" (20-40 words).
- If the answer is not answerable and need more context about the company local data, set "isSimple": false and leave "data" as an empty string.
- Under no circumstances output any text outside the JSON object.
                """,
                """
User Request: {userPrompt}

Relevant Static Context: {context}

answer to the user request as per the RESPONSE RULES above.
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
