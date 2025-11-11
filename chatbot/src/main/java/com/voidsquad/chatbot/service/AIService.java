package com.voidsquad.chatbot.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.voidsquad.chatbot.entities.WorkflowStep;
import com.voidsquad.chatbot.exception.JsonDecodeException;
import com.voidsquad.chatbot.exception.NoAnswerException;
import com.voidsquad.chatbot.repository.WorkflowStepRepository;
import com.voidsquad.chatbot.service.language.provider.ChatClient;
import com.voidsquad.chatbot.service.language.provider.ChatResponse;
import com.voidsquad.chatbot.service.promptmanager.core.ProcessingResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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
    private final WorkflowStepRepository workflowStepRepository;

    public AIService(@Qualifier("geminiChatClient") ChatClient chatClient,
                     @Autowired(required = false) EmbeddingService embeddingService,
                     @Autowired(required = false) StaticInfoRepository staticInfoRepository,
                     @Autowired(required = false) LanguageProcessor languageProcessor,
                     @Autowired(required = false) ObjectMapper objectMapper, WorkflowStepRepository workflowStepRepository) {
        this.chatClient = chatClient;
        this.embeddingService = embeddingService;
        this.staticInfoRepository = staticInfoRepository;
        this.languageProcessor = languageProcessor;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.workflowStepRepository = workflowStepRepository;
    }

    public String generation(String userInput) {
        try {
            ChatResponse resp = this.chatClient
                    .prompt()
                    .system("You are a helpful chatbot assistant.")
                    .user("Explain briefly: " + userInput)
                    .call();

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

    public String requestHandler(String userPrompt) throws JsonDecodeException, NoAnswerException, IOException {
        log.info("user prompt: "+userPrompt);
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

        boolean isSimple = false;
        String strData = "";

        JsonOutputData structuredOutput = getJsonOutputData(simpleResult, objectMapper);

        if(structuredOutput.isSimple()) {
            log.info("simple reply found, returning");
            return structuredOutput.strData();
        }
        else{
            log.info("not a simple reply, need complex processing");
//            return "Complex requests not yet supported";
                return prepareWorkflow(userPrompt,qEmbedding,strData,"Role: CUSTOMER");
        }




    }


    private String prepareWorkflow(String userPrompt, float[] embiddings , String toolCallReason ,String userInfo ) {

        List<WorkflowStep> hits = workflowStepRepository.findSimilarSteps(embiddings,5);
        StringBuilder contextBuilder = new StringBuilder();
        for (WorkflowStep s : hits) {
            contextBuilder.append("Name: ").append(s.getName()).append("\n");
            contextBuilder.append(s.getDescription()).append("\n---\n");
        }
        contextBuilder.append("User Info: ").append(userInfo).append("\n");
        contextBuilder.append("Reason For ToolCalls: ").append(toolCallReason).append("\n");
        String context = contextBuilder.toString();

        var usableTools = languageProcessor.findHelperToolCalls(
                userPrompt,
                context,
                userInfo,
                "false");

        return usableTools.output();
    }

    public List<String> getAllStaticInfoByEmbeddings(String keyword) {
                float[] emb = embeddingService.generateEmbedding(keyword);
                int count = 0;
                return staticInfoRepository.findSimilarStaticInfo(emb,5).stream().map(staticInfo -> {
                    String info = "Topic " + staticInfo.getTopic() + "\n" +
                            "Description: " + staticInfo.getDescription() + "\n";
                    return info;
                }).toList();
            }

    private record JsonOutputData(boolean isSimple, String strData) { }

    private static JsonOutputData getJsonOutputData(ProcessingResult simpleResult, ObjectMapper objectMapper) {
        String strData = "";
        boolean isSimple = false;

        Object data = simpleResult.metadata().get("data");
        if (data != null) {
            strData = data.toString();
        }

        String output = simpleResult.output();
        if (output != null && !output.isEmpty()) {
            // Decode URL-encoded characters if any
            String decoded = output;
            for (int i = 0; i < 3 && decoded.contains("%"); i++) {
                decoded = URLDecoder.decode(decoded, StandardCharsets.UTF_8);
            }
            decoded = decoded.trim();

            // Handle possible text before JSON
            int firstBrace = decoded.indexOf('{');
            if (firstBrace > 0) {
                decoded = decoded.substring(firstBrace);
            }

            try {
                log.info("Parsing JSON from LLM output...");
                JsonNode node = objectMapper.readTree(decoded);

                if (node.has("isSimple")) {
                    isSimple = node.get("isSimple").asBoolean(false);
                }
                if (node.has("data")) {
                    strData = node.get("data").asText("");
                }

                log.info("Parsed result -> isSimple: {}, data: {}", isSimple, strData);

            } catch (Exception e) {
                log.warn("Failed to parse JSON from LLM output: {}", e.getMessage());
            }
        } else {
            throw new NoAnswerException("No output from language model");
        }
        return new JsonOutputData(isSimple, strData);
    }

//    public void send(String message) {
//        rabbitTemplate.convertAndSend(
//                RabbitMQConfig.EXCHANGE,
//                RabbitMQConfig.ROUTING_KEY,
//                message
//        );
//    }

}
