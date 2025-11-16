package com.voidsquad.chatbot.service.tool;

import com.voidsquad.chatbot.model.ToolCall;
import com.voidsquad.chatbot.service.auth.AuthInfo;
import com.voidsquad.chatbot.service.tool.Tool;
import com.voidsquad.chatbot.service.tool.ToolCallRequest;
import com.voidsquad.chatbot.service.tool.ToolCallResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates execution of tools based on decoded `ToolCall` instructions.
 */
@Service
public class ToolExecutionService {
    private static final Logger log = LoggerFactory.getLogger(ToolExecutionService.class);

    private final ToolRegistry registry;

    public ToolExecutionService(ToolRegistry registry) {
        this.registry = registry;
    }

    public List<ToolCallResult> executeAll(List<ToolCall> calls, String userInput ,AuthInfo userInfo) {
        List<ToolCallResult> results = new ArrayList<>();
        if (calls == null) return results;

        for (ToolCall c : calls) {
            if (c == null) continue;
            String toolName = c.getToolName();
            var opt = registry.find(toolName);
            if (opt.isEmpty()) {
                String msg = "Tool not found: " + toolName;
                log.warn(msg);
                results.add(ToolCallResult.failure(msg,toolName));
                continue;
            }

            c.getParameters().put("userInput",userInput);
            c.getParameters().put("userInfo",userInfo);

            Tool t = opt.get();
            try {
                ToolCallRequest req = new ToolCallRequest(c.getParameters());
                ToolCallResult res = t.execute(req);
                results.add(res);
            } catch (Exception ex) {
                log.error("Tool '{}' execution failed: {}", toolName, ex.toString());
                results.add(ToolCallResult.failure("execution error: " + ex.getMessage(), toolName));
            }
        }

        return results;
    }
}
