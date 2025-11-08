package com.voidsquad.chatbot.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
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

    public void addStaticInfo(String topic, String description) {
        try {
            float[] embedding = null;
            if (embeddingService != null) {
                embedding = embeddingService.generateEmbedding(topic + ": " + description);
            }
            else{
                throw new NullPointerException("EmbeddingService is not available");
            }
            StaticInfo staticInfo = new StaticInfo();
            staticInfo.setId(UUID.randomUUID());
            staticInfo.setTopic(topic);
            staticInfo.setDescription(description);
            staticInfo.setEmbedding(embedding);
            staticInfoRepository.saveWithVector(staticInfo);
            log.info("StaticInfo added: " + topic);
        } catch (Exception e) {
            log.error("Error adding StaticInfo: " + e.getMessage());
        }
    }

    public List<StaticInfo> ReadStaticInfoFromCSV(MultipartFile file) throws IOException, CsvException {
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            List<StaticInfo> staticInfoList = new ArrayList<>();
            List<String[]> records = reader.readAll();

            if (records.isEmpty()) return staticInfoList;

            // Read header
            String[] header = records.get(0);
            int topicIndex = -1, descriptionIndex = -1;
            for (int i = 0; i < header.length; i++) {
                if (header[i].equalsIgnoreCase("topic")) topicIndex = i;
                if (header[i].equalsIgnoreCase("description")) descriptionIndex = i;
            }

            if (topicIndex == -1 || descriptionIndex == -1) {
                log.error("CSV missing required headers 'topic' or 'description'");
                return staticInfoList;
            }

            // Read data rows
            for (int i = 1; i < records.size(); i++) {
                String[] record = records.get(i);
                String topic = record.length > topicIndex ? record[topicIndex] : null;
                String description = record.length > descriptionIndex ? record[descriptionIndex] : null;

                if (topic == null || description == null || topic.isBlank() || description.isBlank()) {
                    log.warn("Skipping record due to missing topic or description at row " + (i + 1));
                    continue;
                }

                staticInfoList.add(StaticInfo.builder()
                        .topic(topic)
                        .description(description)
                        .build());
            }

            return staticInfoList;
        }
    }


    public void addBulkStaticInfo(List<StaticInfo> staticInfoList) {
        log.info("Adding bulk StaticInfo: " + staticInfoList.size() + " entries.");
        try {
            for (StaticInfo info : staticInfoList) {
                float[] embedding = null;
                if (embeddingService != null) {
                    embedding = embeddingService.generateEmbedding(info.getTopic() + ": " + info.getDescription());
                } else {
                    throw new NullPointerException("EmbeddingService is not available");
                }
                info.setId(UUID.randomUUID());
                info.setEmbedding(embedding);
                staticInfoRepository.saveWithVector(info);
            }
            log.info("Bulk StaticInfo added: " + staticInfoList.size() + " entries.");
        } catch (Exception e) {
            log.error("Error adding bulk StaticInfo: " + e.getMessage());
        }
    }

    public String requestHandler(String userPrompt){
        log.info("user prompt: "+userPrompt);
        try {
            if (embeddingService == null || staticInfoRepository == null || languageProcessor == null) {
                log.info("direct answer from language model");
                return generation(userPrompt);
            }

            // 1) generate embedding and search static_info_vector_db
            float[] qEmbedding = embeddingService.generateEmbedding(userPrompt);
            log.info("getting embeddings"+(qEmbedding.length==384?" (384-dim)":"(not 384-dim)"));
            List<StaticInfo> hits = staticInfoRepository.findSimilarStaticInfo(qEmbedding, 5);
            log.info("hits:"+hits.size());

            StringBuilder contextBuilder = new StringBuilder();
            for (StaticInfo s : hits) {
                contextBuilder.append("Topic: ").append(s.getTopic()).append("\n");
                contextBuilder.append(s.getDescription()).append("\n---\n");
            }
            String vectorContext = contextBuilder.toString();

            log.info("genarated context: "+vectorContext);

            log.info("sending for simple reply with context");
            // 2) Ask LanguageProcessor to evaluate if a simple reply is possible
            var simpleResult = languageProcessor.evaluateSimpleReply(userPrompt, vectorContext, "USER");
            // simpleResult.metadata contains isSimple flag
            log.info("LM RESULT => "+simpleResult);

            Object isSimpleObj = simpleResult.metadata().get("isSimple");
            boolean isSimple = false;
            if (isSimpleObj instanceof Boolean) isSimple = (Boolean) isSimpleObj;
            else if (isSimpleObj instanceof String) isSimple = Boolean.parseBoolean((String) isSimpleObj);

            if (isSimple) {
                log.info("simple reply found, returning");
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
            log.info("not a simple reply, need complex processing");
            // for now we return error
            return "Complex requests not yet supported";
            // 3) Not simple -> forward to workflow tool identification
//            var toolPlan = languageProcessor.findHelperToolCalls(userPrompt, vectorContext, "USER", null);
//            return toolPlan.output();

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
