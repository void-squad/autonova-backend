package com.voidsquad.chatbot.service.promptmanager.core;

public record PromptConfig(
        String systemPrompt,
        String userPromptTemplate,
        OutputFormat outputFormat,
        double temperature,
        int maxTokens
) {
}