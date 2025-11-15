package com.voidsquad.chatbot.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Container for tool call responses from the LLM.
 */
public class ToolCallStrategyResponse {
    private final List<ToolCall> toolCalls;

    @JsonCreator
    public ToolCallStrategyResponse(@JsonProperty("tool_calls") List<ToolCall> toolCalls) {
        this.toolCalls = toolCalls;
    }

    public List<ToolCall> getToolCalls() { return toolCalls; }
}
