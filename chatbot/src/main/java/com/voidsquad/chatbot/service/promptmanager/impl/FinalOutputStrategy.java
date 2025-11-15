package com.voidsquad.chatbot.service.promptmanager.impl;

import com.voidsquad.chatbot.service.promptmanager.PromptStrategy;
import com.voidsquad.chatbot.service.promptmanager.core.OutputFormat;
import com.voidsquad.chatbot.service.promptmanager.core.ProcessingResult;
import com.voidsquad.chatbot.service.promptmanager.core.ProcessingType;
import com.voidsquad.chatbot.service.promptmanager.core.PromptConfig;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class FinalOutputStrategy implements PromptStrategy {

    @Override
    public PromptConfig getConfig() {
        return new PromptConfig(
                """
                You are a chatbot assistant for an automobile ERP system.
                Generate clear, insightful responses based on the provided data.
                
                Guidelines:
                - Be concise but comprehensive
                - Consider the user's role and context and use appropriate business terminology
                - Format numbers and dates professionally
                
                Structure your response with:
                1. Executive Summary
                2. Key Findings
                3. Supporting Data
                4. Recommendations (if applicable)
                """,
                """
                Original User Request: {userPrompt}
                
                Context and Retrieved Data: {context}
                
                User Information: {userInfo}
                
                Generate a comprehensive business response:
                """,
                OutputFormat.TEXT,
                0.7,
                2000
        );
    }

    @Override
    public String buildUserPrompt(String userInput, String context, Map<String, Object> params) {
        String userInfo = (String) params.getOrDefault("userInfo", "Unknown User");
        return getConfig().userPromptTemplate()
                .replace("{userPrompt}", userInput)
                .replace("{context}", context)
                .replace("{userInfo}", userInfo);
    }

    @Override
    public ProcessingResult postProcess(String llmOutput) {
        // Additional formatting or validation
        String formattedOutput = formatBusinessResponse(llmOutput);
        return new ProcessingResult(
                formattedOutput,
                OutputFormat.TEXT,
                ProcessingType.FINAL_OUTPUT_GENERATION,
                Map.of("word_count", formattedOutput.split("\\s+").length)
        );
    }

    private String formatBusinessResponse(String rawOutput) {
        // Add any business-specific formatting
        return rawOutput;
    }
}
