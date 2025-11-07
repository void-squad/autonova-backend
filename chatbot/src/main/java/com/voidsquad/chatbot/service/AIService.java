package com.voidsquad.chatbot.service;

import com.voidsquad.chatbot.config.RabbitMQConfig;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AIService {

//    @Autowired
//    private RabbitTemplate rabbitTemplate;

    private final ChatClient chatClient;

    public AIService(@Autowired(required = false) ChatClient.Builder chatClientBuilder) {
        this.chatClient = (chatClientBuilder != null)
                ? chatClientBuilder.build()
                : null;
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

    public String requestHandler(String userPrompt){

        return "";
    }


//    public void send(String message) {
//        rabbitTemplate.convertAndSend(
//                RabbitMQConfig.EXCHANGE,
//                RabbitMQConfig.ROUTING_KEY,
//                message
//        );
//    }

}
