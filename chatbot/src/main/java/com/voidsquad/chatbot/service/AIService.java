package com.voidsquad.chatbot.service;

import com.voidsquad.chatbot.config.RabbitMQConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.voidsquad.chatbot.service.embedding.EmbeddingService;
import com.voidsquad.chatbot.repository.StaticInfoRepository;
import com.voidsquad.chatbot.service.language.LanguageProcessor;
import com.voidsquad.chatbot.entities.StaticInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.UUID;

@Service
public class AIService {
    private static final Logger log = LogManager.getLogger(AIService.class);

//    @Autowired
//    private RabbitTemplate rabbitTemplate;

    private final ChatClient chatClient;
    private final EmbeddingService embeddingService;
    private final StaticInfoRepository staticInfoRepository;
    private final LanguageProcessor languageProcessor;
    private final ObjectMapper objectMapper;

    public AIService(@Autowired(required = false) ChatClient.Builder chatClientBuilder,
                     @Autowired(required = false) EmbeddingService embeddingService,
                     @Autowired(required = false) StaticInfoRepository staticInfoRepository,
                     @Autowired(required = false) LanguageProcessor languageProcessor,
                     @Autowired(required = false) ObjectMapper objectMapper) {
        this.chatClient = (chatClientBuilder != null) ? chatClientBuilder.build() : null;
        this.embeddingService = embeddingService;
        this.staticInfoRepository = staticInfoRepository;
        this.languageProcessor = languageProcessor;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
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
        try {
            if (embeddingService == null || staticInfoRepository == null || languageProcessor == null) {
                // Fallback to simple generation if infra missing
                return generation(userPrompt);
            }

            // 1) generate embedding and search static_info_vector_db
            float[] qEmbedding = embeddingService.generateEmbedding(userPrompt);
            List<StaticInfo> hits = staticInfoRepository.findSimilarStaticInfo(qEmbedding, 5);

            StringBuilder contextBuilder = new StringBuilder();
            for (StaticInfo s : hits) {
                contextBuilder.append("Topic: ").append(s.getTopic()).append("\n");
                contextBuilder.append(s.getDescription()).append("\n---\n");
            }
            String vectorContext = contextBuilder.toString();

            // 2) Ask LanguageProcessor to evaluate if a simple reply is possible
            var simpleResult = languageProcessor.evaluateSimpleReply(userPrompt, vectorContext, "USER");
            // simpleResult.metadata contains isSimple flag
            log.info(simpleResult);
            Object isSimpleObj = simpleResult.metadata().get("isSimple");
            boolean isSimple = false;
            if (isSimpleObj instanceof Boolean) isSimple = (Boolean) isSimpleObj;
            else if (isSimpleObj instanceof String) isSimple = Boolean.parseBoolean((String) isSimpleObj);

            if (isSimple) {
                // Return the simple reply data
                Object data = simpleResult.metadata().get("data");
                if (data != null) return data.toString();
                // Fallback: try to parse JSON output
                try {
                    JsonNode node = objectMapper.readTree(simpleResult.output());
                    if (node.has("data")) return node.get("data").asText();
                } catch (Exception ignored) {}
                return simpleResult.output();
            }

            // 3) Not simple -> forward to workflow tool identification
            var toolPlan = languageProcessor.findHelperToolCalls(userPrompt, vectorContext, "USER", null);
            return toolPlan.output();

        } catch (Exception e) {
            System.err.println("AIService.requestHandler error: " + e.getMessage());
            return "Error processing request";
        }
    }


//    public void send(String message) {
//        rabbitTemplate.convertAndSend(
//                RabbitMQConfig.EXCHANGE,
//                RabbitMQConfig.ROUTING_KEY,
//                message
//        );
//    }

}
