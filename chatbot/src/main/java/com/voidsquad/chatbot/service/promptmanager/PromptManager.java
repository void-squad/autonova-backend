package com.voidsquad.chatbot.service.promptmanager;

import com.voidsquad.chatbot.service.promptmanager.core.ProcessingRequest;
import com.voidsquad.chatbot.service.promptmanager.core.ProcessingType;
import com.voidsquad.chatbot.service.promptmanager.core.PromptConfig;
import com.voidsquad.chatbot.service.promptmanager.impl.FinalOutputStrategy;
import com.voidsquad.chatbot.service.promptmanager.impl.ToolCallStrategy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PromptManager {

    private final Map<ProcessingType, PromptStrategy> strategies;

    public PromptManager(List<PromptStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(
                        this::getStrategyType,
                        Function.identity()
                ));
    }

    private ProcessingType getStrategyType(PromptStrategy strategy) {
        if (strategy instanceof ToolCallStrategy) return ProcessingType.TOOL_CALL_IDENTIFICATION;
        if (strategy instanceof FinalOutputStrategy) return ProcessingType.FINAL_OUTPUT_GENERATION;
        // Simple chat quick-reply strategy
        if (strategy.getClass().getSimpleName().equals("SimpleChatStrategy")) return ProcessingType.SIMPLE_CHAT;
        // Add other mappings
        throw new IllegalArgumentException("Unknown strategy type");
    }

    public PromptConfig getPromptConfig(ProcessingType type) {
        return getStrategy(type).getConfig();
    }

    public String buildUserPrompt(ProcessingRequest request) {
        PromptStrategy strategy = getStrategy(request.type());
        return strategy.buildUserPrompt(
                request.userPrompt(),
                request.context(),
                request.additionalParams()
        );
    }


    private PromptStrategy getStrategy(ProcessingType type) {
        return Optional.ofNullable(strategies.get(type))
                .orElseThrow(() -> new IllegalArgumentException("No strategy for type: " + type));
    }

    public com.voidsquad.chatbot.service.promptmanager.core.ProcessingResult postProcess(ProcessingType type, String llmOutput) {
        PromptStrategy strategy = getStrategy(type);
        return strategy.postProcess(llmOutput);
    }
}
