package com.voidsquad.chatbot.decoder;

/**
 * Simple DTO representing the normalized final output produced by the LLM final-output strategy.
 */
public class FinalOutputStrategyResponse {
    private final boolean isComplete;
    private final String data;

    public FinalOutputStrategyResponse(boolean isComplete, String data) {
        this.isComplete = isComplete;
        this.data = data == null ? "" : data;
    }

    public boolean isComplete() {
        return isComplete;
    }

    public String getData() {
        return data;
    }
}
