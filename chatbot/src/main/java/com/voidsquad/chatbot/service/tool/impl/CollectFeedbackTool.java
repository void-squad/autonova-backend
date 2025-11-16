package com.voidsquad.chatbot.service.tool.impl;

import com.voidsquad.chatbot.entities.Feedback;
import com.voidsquad.chatbot.repository.FeedbackRepository;
import com.voidsquad.chatbot.service.tool.Tool;
import com.voidsquad.chatbot.service.tool.ToolCallRequest;
import com.voidsquad.chatbot.service.tool.ToolCallResult;
import com.voidsquad.chatbot.service.auth.AuthInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;

@Component
public class CollectFeedbackTool implements Tool {

    private final FeedbackRepository feedbackRepository;
    private static final Logger log = LoggerFactory.getLogger(CollectFeedbackTool.class);

    public CollectFeedbackTool(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    @Override
    public String name() {
        return "collectFeedbacks";
    }

    @Override
    public ToolCallResult execute(ToolCallRequest request) {
        Map<String, Object> params = request.getParameters();

        // Prefer authenticated userId when available
        Long userId = null;
        if (params.containsKey("userInfo") && params.get("userInfo") instanceof AuthInfo) {
            userId = params.get("userInfo") != null ?
                    ((AuthInfo) params.get("userInfo")).getUserId() : null;
        }


        Object feedbackObj = params.getOrDefault("userInput", null);
        String userInput = feedbackObj == null ? null : String.valueOf(feedbackObj);


        if (userInput == null || userInput.trim().isEmpty()) {
            return ToolCallResult.failure("No feedback text provided", name());
        }

        try {
            Feedback fb = Feedback.builder()
                    .feedbackId(UUID.randomUUID())
                    .feedback(userInput)
                    .userId(userId)
                    .createdDate(OffsetDateTime.now())
                    .build();

            feedbackRepository.save(fb);

            return ToolCallResult.success("Thank you for your feedback", name());
        } catch (Exception e) {
            log.error("Failed to persist feedback: {}", e.getMessage());
            return ToolCallResult.failure("Failed to persist feedback", name());
        }
    }

}
