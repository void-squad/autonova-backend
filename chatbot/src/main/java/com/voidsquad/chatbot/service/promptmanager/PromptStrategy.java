package com.voidsquad.chatbot.service.promptmanager;

import com.voidsquad.chatbot.service.promptmanager.core.ProcessingResult;
import com.voidsquad.chatbot.service.promptmanager.core.PromptConfig;

import java.util.Map;

public interface PromptStrategy {
    PromptConfig getConfig();
    String buildUserPrompt(String userInput, String context, Map<String, Object> params);
    ProcessingResult postProcess(String llmOutput);
}
