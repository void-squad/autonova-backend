package com.voidsquad.chatbot.service.language;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voidsquad.chatbot.service.auth.AuthInfo;
import com.voidsquad.chatbot.service.language.provider.ChatClient;
import com.voidsquad.chatbot.service.language.provider.ChatResponse;
import com.voidsquad.chatbot.service.promptmanager.PromptManager;
import com.voidsquad.chatbot.service.promptmanager.core.ProcessingRequest;
import com.voidsquad.chatbot.service.promptmanager.core.ProcessingResult;
import com.voidsquad.chatbot.service.promptmanager.core.ProcessingType;
import com.voidsquad.chatbot.service.promptmanager.core.PromptConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class LanguageProcessor {

    private final PromptManager promptManager;
    private ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private static final Logger log = LogManager.getLogger(LanguageProcessor.class);


    public LanguageProcessor(PromptManager promptManager,
                             @Qualifier("ollamaChatClient") ChatClient ollamaChatClientBuilder,
                             @Qualifier("geminiChatClient") ChatClient geminiChatClientBuilder,
                             @Value("${app.llm.provider}") LLMProvider llmProvider,
                             ObjectMapper objectMapper) {
        this.promptManager = promptManager;
        if(llmProvider.equals(LLMProvider.GEMINI)) {
            log.info("Initializing Gemini API ...");
            this.chatClient = geminiChatClientBuilder;
        }else if(llmProvider.equals(LLMProvider.OLLAMA)) {
            log.info("Initializing Local Ollama ...");
            this.chatClient = ollamaChatClientBuilder;
        }
        else {
            throw new IllegalArgumentException("Unsupported LLM provider: " + llmProvider);
        }
        this.objectMapper = objectMapper;
        if(this.chatClient == null) {
            throw new IllegalArgumentException("ChatClient initialization failed for provider: " + llmProvider);
        }
    }

    private String sanitizeInput(String input) {
        String tmp = input
                .replace("{", "%7B")
                .replace("}", "%7D")
                .replace("|", "%7C")
                .trim();
        return tmp;
    }

    private ProcessingResult processUserMessage(ProcessingRequest request) {
        try {
            // Build user prompt
            log.info("Processing request of type: " + request.type());
            String userPrompt = promptManager.buildUserPrompt(request);
            // Get config for temperature/maxTokens
            PromptConfig config = promptManager.getPromptConfig(request.type());
            log.info("Prompt config retrieved: " + config.systemPrompt() , config.outputFormat(), config.userPromptTemplate(), config.maxTokens());

            // Call LLM with custom system prompt
            String llmOutput = callLanguageModel(sanitizeInput(config.systemPrompt()), sanitizeInput(userPrompt), config);
            log.info("LLM output received==> " + llmOutput);

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

        return processUserMessage(request);
    }

    public ProcessingResult findHelperToolCalls(String userPrompt, String vectorContext, String userRole) {

        ProcessingRequest request = new ProcessingRequest(
                userPrompt,
                vectorContext,
                ProcessingType.TOOL_CALL_IDENTIFICATION,
                Map.of("userRole", userRole)
        );
        return processUserMessage(request);
    }

    public ProcessingResult finalOutputPrepWithData(String userPrompt, String extractedContext, String userInfo) {
        ProcessingRequest request = new ProcessingRequest(
                userPrompt,
                extractedContext,
                ProcessingType.FINAL_OUTPUT_GENERATION,
                Map.of("userInfo", userInfo != null ? userInfo : "GUEST")
        );
        return processUserMessage(request);
    }

    private String callLanguageModel(String systemPrompt, String userPrompt, PromptConfig config) {
        log.info("Calling language model with system prompt and user prompt");
        log.info("System Prompt: " + systemPrompt);
        log.info("User Prompt: " + userPrompt);
        ChatResponse response = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call();
        log.info("Language model call completed");

        if (response != null) {
            log.info("Received response from language model");
            return response.getResult().getOutput().getText();
        } else {
            log.warn("No response from language model");
            return "No response from model";
        }
    }

}