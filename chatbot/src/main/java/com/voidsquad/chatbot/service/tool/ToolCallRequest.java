package com.voidsquad.chatbot.service.tool;

import java.util.Collections;
import java.util.Map;

/**
 * Simple immutable request wrapper passed to Tool implementations.
 */
public class ToolCallRequest {
    private final Map<String, Object> parameters;
    private final String callerId; // optional: user or system that requested this

    public ToolCallRequest(Map<String, Object> parameters) {
        this(parameters, null);
    }

    public ToolCallRequest(Map<String, Object> parameters, String callerId) {
        this.parameters = parameters == null ? Collections.emptyMap() : Collections.unmodifiableMap(parameters);
        this.callerId = callerId;
    }

    public Map<String, Object> getParameters() { return parameters; }
    public String getCallerId() { return callerId; }
}
