package com.voidsquad.chatbot.service.language.provider.impl.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class VertexApiService {

    private final WebClient webClient;

    private final String apikey;
    private final String model;

    public VertexApiService(
            @Value("${app.llm.gemini.model}") String model,
            @Value("${app.llm.gemini.apikey}") String apikey
    ) {
        this.apikey=apikey;
        this.model=model;

        this.webClient = WebClient.builder()
//                .baseUrl("https://generativelanguage.googleapis.com/v1beta/models")
                .baseUrl("https://generativelanguage.googleapis.com/v1beta/openai/chat/completions")
                .build();
    }

    public String callVertexApiWithApiKey(String jsonBody) {

        var req = webClient.post()
//                .uri("/{model}:generateContent", this.model)
//                .header("X-goog-api-key", this.apikey)
                .header("Authorization", "Bearer "+this.apikey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(jsonBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        return req;
    }
}