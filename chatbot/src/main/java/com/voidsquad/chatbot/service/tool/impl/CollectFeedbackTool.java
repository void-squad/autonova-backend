package com.voidsquad.chatbot.service.tool.impl;

import com.voidsquad.chatbot.service.tool.Tool;
import com.voidsquad.chatbot.service.tool.ToolCallRequest;
import com.voidsquad.chatbot.service.tool.ToolCallResult;
import org.springframework.stereotype.Component;

@Component
public class CollectFeedbackTool implements Tool {
    @Override
    public String name() {
        return "collectFeedbacks";
    }

    @Override
    public ToolCallResult execute(ToolCallRequest request) {
        return new ToolCallResult(true,"Feedback collected successfully.","Thank you for your feedback!", name());
    }

}
