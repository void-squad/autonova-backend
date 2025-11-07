package com.voidsquad.chatbot.service.promptmanager.core;

public enum ProcessingType {
    TOOL_CALL_IDENTIFICATION,  // Find helper tools for context
    FINAL_OUTPUT_GENERATION,   // Generate final response with data
    DATA_SUMMARY,              // Generate summaries from data
    UI_GUIDANCE,               // Provide navigation guidance
    BUSINESS_INTELLIGENCE      // Generate BI insights
}