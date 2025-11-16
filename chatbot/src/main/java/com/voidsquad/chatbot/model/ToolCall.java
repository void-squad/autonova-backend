package com.voidsquad.chatbot.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Single tool call entry returned by the LLM.
 */
public class ToolCall {
    private final String toolName;
    private final Map<String, Object> parameters;
    private final String explanation;

    @JsonCreator
    public ToolCall(
            @JsonProperty("tool_name") String toolName,
            @JsonProperty("parameters") Map<String, Object> parameters,
            @JsonProperty("explanation") String explanation) {
        this.toolName = toolName;
        this.parameters = parameters;
        this.explanation = explanation;
    }

    public String getToolName() { return toolName; }
    public Map<String, Object> getParameters() { return parameters; }
    public String getExplanation() { return explanation; }
}
