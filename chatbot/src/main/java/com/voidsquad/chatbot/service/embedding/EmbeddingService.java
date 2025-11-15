package com.voidsquad.chatbot.service.embedding;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class EmbeddingService {

    private final RestTemplate restTemplate;
    private final String OLLAMA_URL = "http://localhost:11434/api/embeddings";

    public EmbeddingService() {
        this.restTemplate = new RestTemplate();
    }

    public float[] generateEmbedding(String text) {
        try {
            Map<String, Object> request = Map.of(
                    "model", "all-minilm",  // Embedding model
                    "prompt", text
            );

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    OLLAMA_URL, request, Map.class);

            List<Double> embeddingList = (List<Double>) response.getBody().get("embedding");

            // Convert to float[]
            float[] embedding = new float[embeddingList.size()];
            for (int i = 0; i < embeddingList.size(); i++) {
                embedding[i] = embeddingList.get(i).floatValue();
            }

            return embedding;

        } catch (Exception e) {
            throw new RuntimeException("Embedding generation failed: " + e.getMessage(), e);
        }
    }
}