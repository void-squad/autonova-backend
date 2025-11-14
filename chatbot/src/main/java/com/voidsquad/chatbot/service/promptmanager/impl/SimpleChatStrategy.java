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
You are a concise AI assistant. Respond **only** in valid JSON format.

RESPONSE FORMAT:
{"isSimple": true|false, "data": "..."}

You are a strict JSON-only assistant.
     Output must be exactly:
     {"isSimple": true|false, "data": "..."}

     RULES (follow exactly):
     A. NEVER output anything outside JSON.
     B. If the message is about ANY of the following, set "isSimple": false:
        - company projects (ongoing or completed)
        - sales, revenue, or performance data
        - inventory or product availability
        - financial reports or summaries
        - ANY feedback, complaint, suggestion, or thank-you message from a user
     C. When "isSimple" = false:
        - Always give a short reason inside "data", like
          {"isSimple": false, "data": "User feedback detected, requires saving."}
     D. When "isSimple" = true:
        - Give a complete, 20–40 word answer.
        - If insufficient info, respond:
          {"isSimple": true, "data": "I do not have enough information to answer that question."}
     E. Never ignore rule B. Even polite or positive sentences count as feedback.
""",
                """
User Request: {userPrompt}

Relevant Static Context: {context}

answer to the user request as per the RESPONSE RULES above.
                """,
                OutputFormat.JSON,
                0.3,
                200
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
