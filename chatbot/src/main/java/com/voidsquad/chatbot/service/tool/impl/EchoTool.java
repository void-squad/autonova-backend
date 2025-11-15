package com.voidsquad.chatbot.service.tool.impl;

import com.voidsquad.chatbot.service.auth.AuthInfo;
import com.voidsquad.chatbot.service.tool.Tool;
import com.voidsquad.chatbot.service.tool.ToolCallRequest;
import com.voidsquad.chatbot.service.tool.ToolCallResult;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Example tool for local testing: echoes back input parameters as a string.
 */
@Component
public class EchoTool implements Tool {
    @Override
    public String name() { return "echo"; }

    @Override
    public ToolCallResult execute(ToolCallRequest request) {
        Map<String, Object> p = request.getParameters();
        return ToolCallResult.success(p.toString(),name());
    }
}
