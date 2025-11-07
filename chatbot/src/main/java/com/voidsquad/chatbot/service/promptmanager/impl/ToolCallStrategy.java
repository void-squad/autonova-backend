package com.voidsquad.chatbot.service.promptmanager.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voidsquad.chatbot.service.promptmanager.PromptStrategy;
import com.voidsquad.chatbot.service.promptmanager.core.OutputFormat;
import com.voidsquad.chatbot.service.promptmanager.core.ProcessingResult;
import com.voidsquad.chatbot.service.promptmanager.core.ProcessingType;
import com.voidsquad.chatbot.service.promptmanager.core.PromptConfig;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ToolCallStrategy implements PromptStrategy {

    ObjectMapper objectMapper;

    @Override
    public PromptConfig getConfig() {
        return new PromptConfig(
                """
You are a tool identification expert. Analyze the user request and available context 
            to identify what tools/functions need to be called and in what order.
            
            ALWAYS AVAILABLE BASIC TOOLS:
            - getBusinessContactInfo(): Gets company contact information
            - collectFeedbacks(): Collects customer feedback data
            - collectComplains(): Gathers customer complaints data
            
            CONTEXT-SPECIFIC TOOLS (from vector database):
            {systemContext}
            
            IMPORTANT RULES:
            1. Use basic tools only for contact info, feedback, or complaint requests
            2. Use context-specific tools when provided in the context
            3. If context provides tools, prioritize them over basic tools
            4. Only suggest tools that are relevant to the user's request
            5. Consider tool dependencies and execution order
            
            Output MUST be valid JSON with this structure:
            {
                "tool_calls": [
                    {
                        "tool_name": "string",
                        "parameters": {"param1": "value1"},
                        "description": "what this call will achieve",
                        "depends_on": ["previous_tool_output"]
                    }
                ],
                "reasoning": "brief explanation of why these tools were chosen"
            }
            """,
                """
                User Request: {userPrompt}
                
                Available Context Tools: {context}
                
                User Role: {userRole}
                
                Analyze and provide the tool execution plan. Use basic tools only for basic requests.
                If context provides specialized tools, use those instead.
                """,
                OutputFormat.JSON,
                0.2,
                1500
        );
    }

    @Override
    public String buildUserPrompt(String userInput, String context, Map<String, Object> params) {
        String userRole = (String) params.getOrDefault("userRole", "USER");
        return getConfig().userPromptTemplate()
                .replace("{userPrompt}", userInput)
                .replace("{context}", context)
                .replace("{userRole}", userRole);
    }

    @Override
    public String buildSystemPrompt(String systemContext) {
        return getConfig().systemPrompt()
                .replace("{systemContext}", systemContext != null ? systemContext : "No additional context tools provided");
    }

    @Override
    public ProcessingResult postProcess(String llmOutput) {
        try {
            // Parse and validate JSON
            JsonNode jsonOutput = objectMapper.readTree(llmOutput);
            return new ProcessingResult(
                    llmOutput,
                    OutputFormat.JSON,
                    ProcessingType.TOOL_CALL_IDENTIFICATION,
                    Map.of("tool_count", jsonOutput.get("tool_calls").size())
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse tool call output", e);
        }
    }

}