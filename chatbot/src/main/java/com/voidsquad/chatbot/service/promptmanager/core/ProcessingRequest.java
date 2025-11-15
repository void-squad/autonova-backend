package com.voidsquad.chatbot.service.promptmanager.core;

import java.util.Map;

public record ProcessingRequest(
        String userPrompt,
        String context,
        ProcessingType type,
        Map<String, Object> additionalParams
) {}