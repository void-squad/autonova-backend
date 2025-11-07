package com.voidsquad.chatbot.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

@Service
public class AIService {

    private final ChatClient chatClient;

    public AIService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public String generation(String userInput) {
        try {
            ChatResponse resp = this.chatClient
                    .prompt()
                    .system("You are a helpful chatbot assistant.")
                    .user("Explain briefly: " + userInput)
                    .call()
                    .chatResponse();

            if (resp != null) {
                return resp.getResult().getOutput().getText();
            } else {
                return "No response from model";
            }

        } catch (Exception e) {
            System.err.println("AIService.generation error: " + e.getMessage());
            return "Error!";
        }
    }
}
