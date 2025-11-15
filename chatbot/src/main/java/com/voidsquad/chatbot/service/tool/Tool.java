package com.voidsquad.chatbot.service.tool;

/**
 * Generic tool contract. Implementations should be stateless and thread-safe.
 */
public interface Tool {
    /**
     * A short unique name for the tool (used to look it up).
     */
    String name();

    /**
     * Execute the tool with the provided request.
     */
    ToolCallResult execute(ToolCallRequest request);
}
