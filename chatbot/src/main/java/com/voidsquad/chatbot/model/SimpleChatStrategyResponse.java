package com.voidsquad.chatbot.model;

/**
 * Result of decoding a simple LLM JSON response.
 */
public record SimpleChatStrategyResponse(boolean isSimple, String data) { }
