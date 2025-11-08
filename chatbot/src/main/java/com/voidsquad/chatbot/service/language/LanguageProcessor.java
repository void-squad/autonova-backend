package com.voidsquad.chatbot.service.language;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voidsquad.chatbot.service.promptmanager.PromptManager;
import com.voidsquad.chatbot.service.promptmanager.PromptStrategy;
import com.voidsquad.chatbot.service.promptmanager.core.ProcessingRequest;
import com.voidsquad.chatbot.service.promptmanager.core.ProcessingResult;
import com.voidsquad.chatbot.service.promptmanager.core.ProcessingType;
import com.voidsquad.chatbot.service.promptmanager.core.PromptConfig;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class LanguageProcessor {

    private final PromptManager promptManager;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public LanguageProcessor(PromptManager promptManager,
                             ChatClient.Builder chatClientBuilder,
                             ObjectMapper objectMapper) {
        this.promptManager = promptManager;
        this.chatClient = (chatClientBuilder != null)
                ? chatClientBuilder.build()
                : null;
        this.objectMapper = objectMapper;
    }

    private ProcessingResult processWithCustomSystemPrompt(ProcessingRequest request, String systemPrompt) {
        try {
            // Build user prompt
            String userPrompt = promptManager.buildUserPrompt(request);

            // Get config for temperature/maxTokens
            PromptConfig config = promptManager.getPromptConfig(request.type());

            // Call LLM with custom system prompt
            String llmOutput = callLanguageModel(systemPrompt, userPrompt, config);

            // Post-process
            return promptManager.postProcess(request.type(), llmOutput);

        } catch (Exception e) {
            throw new RuntimeException("Language processing failed", e);
        }
    }

    public ProcessingResult evaluateSimpleReply(String userPrompt, String vectorContext, String userRole) {
        ProcessingRequest request = new ProcessingRequest(
                userPrompt,
                vectorContext,
                ProcessingType.SIMPLE_CHAT,
                Map.of("userRole", userRole)
        );

        return processWithCustomSystemPrompt(request, vectorContext);
    }

    public ProcessingResult findHelperToolCalls(String userPrompt, String vectorContext, String userRole, String useFullTools) {
        ProcessingRequest request = new ProcessingRequest(
                userPrompt,
                vectorContext,
                ProcessingType.TOOL_CALL_IDENTIFICATION,
                Map.of("userRole", userRole)
        );
        return processWithCustomSystemPrompt(request,useFullTools);
    }

    public ProcessingResult finalOutputPrepWithData(String userPrompt, String extractedContext, String userInfo) {
        ProcessingRequest request = new ProcessingRequest(
                userPrompt,
                extractedContext,
                ProcessingType.FINAL_OUTPUT_GENERATION,
                Map.of("userInfo", userInfo)
        );
        return processWithCustomSystemPrompt(request,null);
    }

    private String callLanguageModel(String systemPrompt, String userPrompt, PromptConfig config) {
        ChatResponse response = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .chatResponse();

        return response.getResult().getOutput().getText();
    }

    private PromptStrategy getStrategyForType(ProcessingType type) {
        // Deprecated - PromptManager now exposes postProcess; keep for backward compatibility
        return null;
    }
}