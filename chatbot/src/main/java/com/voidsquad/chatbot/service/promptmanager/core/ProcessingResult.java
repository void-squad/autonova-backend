package com.voidsquad.chatbot.service.promptmanager.core;

import java.util.Map;

public record ProcessingResult(
        String output,
        OutputFormat format,
        ProcessingType type,
        Map<String, Object> metadata
) {}