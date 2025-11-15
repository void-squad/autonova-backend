package com.voidsquad.chatbot.service.language.provider.impl;

import com.google.api.client.json.Json;
import com.google.api.client.json.JsonGenerator;
import com.google.cloud.vertexai.VertexAI;
import com.voidsquad.chatbot.service.language.provider.ChatClient;
import com.voidsquad.chatbot.service.language.provider.ChatResponse;
import com.voidsquad.chatbot.service.language.provider.ChatResponseWrapper;
import com.voidsquad.chatbot.service.language.provider.impl.api.AiRequestBuilder;
import com.voidsquad.chatbot.service.language.provider.impl.api.GeminiResponseDecoder;
import com.voidsquad.chatbot.service.language.provider.impl.api.VertexApiService;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Log4j2
@Component
@Qualifier("geminiChatClient")
public class GeminiChatClient implements ChatClient {

    private final VertexApiService vertexApiService;
    private final String modelName;


    public GeminiChatClient(
            VertexApiService vertexApiService,
            @Value("${app.llm.gemini.model}") String geminiModelName
            ) {
        this.vertexApiService = vertexApiService;
        this.modelName = geminiModelName;
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
                AiRequestBuilder requestBuilder = AiRequestBuilder.create()
                        .model(modelName)
                        .message("system", systemPrompt)
                        .message("user", userPrompt);
                var jsonRequest = requestBuilder.buildJson();
                String response = vertexApiService.callVertexApiWithApiKey(jsonRequest);
                GeminiResponseDecoder.DecodedResponse decoded = GeminiResponseDecoder.decode(response);
                return new ChatResponse(decoded.content());
            }
        };
    }

}

