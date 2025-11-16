package com.voidsquad.chatbot.service.promptmanager.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voidsquad.chatbot.util.TextUtil;
import com.voidsquad.chatbot.service.promptmanager.PromptStrategy;
import com.voidsquad.chatbot.service.promptmanager.core.OutputFormat;
import com.voidsquad.chatbot.service.promptmanager.core.ProcessingResult;
import com.voidsquad.chatbot.service.promptmanager.core.ProcessingType;
import com.voidsquad.chatbot.service.promptmanager.core.PromptConfig;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ToolCallStrategy implements PromptStrategy {

  private final ObjectMapper objectMapper;

  public ToolCallStrategy(ObjectMapper objectMapper) {
    // prefer injected ObjectMapper, but fallback to a new instance to avoid NPEs
    this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
  }

    @Override
    public PromptConfig getConfig() {
        return new PromptConfig(
                """
                        You are a tool-identification engine. \s
                        Your output MUST be valid JSON only. \s
                        Never include text outside the JSON.
                        
                        === TASK ===
                        Analyze the user's request and determine which tools/functions should be called.
                        
                        === AVAILABLE BASIC TOOLS ===
                        - collectFeedbacks()
                        - getActiveProjects()
                        
                        === RULES ===
                        3. If additional context-specific tools exist, they will be provided in the context.
                        4. Suggest only relevant tools. If none apply, return an empty tool_calls array.
                        5. Follow tool dependency order when needed.
                        6. Output JSON only and do not include any explanations outside the JSON or formatting keywords.
                        
                        {
                          "tool_calls": [
                            {
                              "tool_name": "string",
                              "parameters": {},
                              "explanation": "Short continuous-tense explanation."
                            }
                          ]
                        }
                        
                        If multiple tools are needed, list them in order.
                        If no tool is needed, return:
                        
                        {
                          "tool_calls": []
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
                1000
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
    public ProcessingResult postProcess(String llmOutput) {
    try {
      String cleaned = TextUtil.stripCodeFences(llmOutput);
      JsonNode jsonOutput = objectMapper.readTree(cleaned);
            return new ProcessingResult(
          cleaned,
                    OutputFormat.JSON,
                    ProcessingType.TOOL_CALL_IDENTIFICATION,
                    Map.of("tool_count", jsonOutput.get("tool_calls").size())
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse tool call output", e);
        }
    }

}