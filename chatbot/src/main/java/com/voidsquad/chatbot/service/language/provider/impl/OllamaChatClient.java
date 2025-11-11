package com.voidsquad.chatbot.service.language.provider.impl;

import com.voidsquad.chatbot.service.language.provider.ChatClient;
import com.voidsquad.chatbot.service.language.provider.ChatResponse;
import com.voidsquad.chatbot.service.language.provider.ChatResponseWrapper;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Qualifier("ollamaChatClient")
public class OllamaChatClient implements ChatClient {

    private final OllamaChatModel ollamaChatModel;

    public OllamaChatClient(
            @Value("${app.llm.ollama.apiUrl:http://localhost:11434}") String ollamaApiUrl
    ) {
        OllamaApi ollamaApi = OllamaApi.builder().baseUrl(ollamaApiUrl).build();
        this.ollamaChatModel = OllamaChatModel.builder().ollamaApi(ollamaApi).build();
    }

    @Override
    public ChatResponseWrapper prompt() {
        return new ChatResponseWrapper() {
            private String systemPrompt;
            private String userPrompt;

            @Override
            public ChatResponseWrapper system(String text) {
                this.systemPrompt = text;
                return this;
            }

            @Override
            public ChatResponseWrapper user(String text) {
                this.userPrompt = text;
                return this;
            }

            @Override
            public ChatResponse call() {
                Prompt prompt = Prompt.builder()
                        .messages(
                                List.of(
                                        SystemMessage.builder().text(systemPrompt).build(),
                                        UserMessage.builder().text(userPrompt).build()
                                )
                        ).build();
                org.springframework.ai.chat.model.ChatResponse chatResponse = ollamaChatModel.call(prompt);
                return new ChatResponse(chatResponse.getResult().getOutput().getText());
            }
        };
    }
}
