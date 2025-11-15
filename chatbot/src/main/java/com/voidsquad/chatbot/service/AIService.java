package com.voidsquad.chatbot.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.voidsquad.chatbot.decoder.SimpleAiResponseDecoder;
import com.voidsquad.chatbot.decoder.ToolCallResponseDecoder;
import com.voidsquad.chatbot.entities.WorkflowStep;
import com.voidsquad.chatbot.exception.JsonDecodeException;
import com.voidsquad.chatbot.exception.NoAnswerException;
import com.voidsquad.chatbot.model.SimpleChatStrategyResponse;
import com.voidsquad.chatbot.model.ToolCall;
import com.voidsquad.chatbot.repository.WorkflowStepRepository;
import com.voidsquad.chatbot.service.auth.AuthInfo;
import com.voidsquad.chatbot.service.promptmanager.core.ProcessingResult;
import com.voidsquad.chatbot.service.tool.ToolCallResult;
import com.voidsquad.chatbot.service.tool.ToolExecutionService;
import com.voidsquad.chatbot.service.tool.ToolRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.voidsquad.chatbot.service.embedding.EmbeddingService;
import com.voidsquad.chatbot.repository.StaticInfoRepository;
import com.voidsquad.chatbot.service.language.LanguageProcessor;
import com.voidsquad.chatbot.entities.StaticInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final EmbeddingService embeddingService;
    private final StaticInfoRepository staticInfoRepository;
    private final LanguageProcessor languageProcessor;
    private final ObjectMapper objectMapper;
    private final WorkflowStepRepository workflowStepRepository;
    private final SimpleAiResponseDecoder simpleAiResponseDecoder;
    private final ToolCallResponseDecoder toolCallResponseDecoder;
    private final ToolRegistry toolRegistry;
    private final ToolExecutionService toolExecutionService;

    public AIService(
            @Autowired(required = false) EmbeddingService embeddingService,
            @Autowired(required = false) StaticInfoRepository staticInfoRepository,
            @Autowired(required = false) LanguageProcessor languageProcessor,
            @Autowired(required = false) ObjectMapper objectMapper,
            @Autowired(required = false) SimpleAiResponseDecoder simpleAiResponseDecoder,
            WorkflowStepRepository workflowStepRepository,
        ToolCallResponseDecoder toolCallResponseDecoder,
        ToolRegistry toolRegistry,
            ToolExecutionService toolExecutionService) {
        this.embeddingService = embeddingService;
        this.staticInfoRepository = staticInfoRepository;
        this.languageProcessor = languageProcessor;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.simpleAiResponseDecoder = simpleAiResponseDecoder != null ? simpleAiResponseDecoder : new SimpleAiResponseDecoder(new com.voidsquad.chatbot.util.JsonPathKeyDecoder());
        this.workflowStepRepository = workflowStepRepository;
        this.toolCallResponseDecoder = toolCallResponseDecoder;
        this.toolRegistry = toolRegistry;
        this.toolExecutionService = toolExecutionService;
    }

    public String generation(String userInput) {
        try {
            ProcessingResult resp = languageProcessor.evaluateSimpleReply(userInput,"You are a chatbot assistant","USER");

            if (resp != null) {
                return resp.output().toString();
            } else {
                return "No response from model";
            }

        } catch (Exception e) {
            log.error("AIService.generation error: {}", e.getMessage(), e);
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

    public String requestHandler(String userPrompt, AuthInfo authInfo) throws JsonDecodeException, NoAnswerException, IOException {
        log.info("user prompt: "+userPrompt);
            if (embeddingService == null || staticInfoRepository == null || languageProcessor == null) {
                log.info("direct answer from language model");
                return generation(userPrompt);
            }

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

            String role = authInfo != null && authInfo.getRole() != null ? authInfo.getRole() : "GUEST";

            var simpleResult = languageProcessor.evaluateSimpleReply(userPrompt, vectorContext, role);
            log.info("LM RESULT => "+simpleResult);

        SimpleChatStrategyResponse structuredOutput = simpleAiResponseDecoder.decode(simpleResult);

        if(structuredOutput.isSimple()) {
            log.info("simple reply found, returning");
            return structuredOutput.data();
        } else {
            log.info("not a simple reply, need complex processing");
            return prepareWorkflow(userPrompt,qEmbedding,structuredOutput.data(), authInfo);
        }




    }


    private String prepareWorkflow(String userPrompt, float[] embiddings , String toolCallReason ,AuthInfo userInfo ) {

        List<WorkflowStep> hits = workflowStepRepository.findSimilarSteps(embiddings,5);
        StringBuilder contextBuilder = new StringBuilder();
        for (WorkflowStep s : hits) {
            contextBuilder.append("Name: ").append(s.getName()).append("\n");
            contextBuilder.append(s.getDescription()).append("\n---\n");
        }
        contextBuilder.append("Reason For ToolCalls: ").append(toolCallReason).append("\n");
        String context = contextBuilder.toString();

        StringBuilder userInfoBuilder = new StringBuilder();
        userInfoBuilder.append("User Info: ").append("\n")
                .append("firstName: ").append(userInfo.getFirstName()).append("\n")
                .append("role: ").append(userInfo.getRole()).append("\n")
                .append("userId: ").append(userInfo.getUserId()).append("\n")
                .append("\n---\n");
        String userInfoStr = userInfoBuilder.toString();

        var usableTools = languageProcessor.findHelperToolCalls(
                userPrompt,
                context,
                userInfoStr);
        String output = usableTools.output();
        List<ToolCall> toolCalls = toolCallResponseDecoder.decode(output);

        List<ToolCallResult> toolCallResults = toolExecutionService.executeAll(toolCalls);

        StringBuilder toolResultsBuilder = new StringBuilder();
        for( ToolCallResult result : toolCallResults ){
            toolResultsBuilder.append("\n--- Tool: ").append(result.getToolName()).append(" ---\n");
            toolResultsBuilder.append(result.getResult()).append("\n");
        }

        return toolResultsBuilder.toString();
    }


    // Tool beans are discovered and registered by Spring into ToolRegistry at startup.
    // No runtime registration required; keep this method removed to avoid mutation of the registry.

    public List<String> getAllStaticInfoByEmbeddings(String keyword) {
                float[] emb = embeddingService.generateEmbedding(keyword);
                int count = 0;
                return staticInfoRepository.findSimilarStaticInfo(emb,5).stream().map(staticInfo -> {
                    String info = "Topic " + staticInfo.getTopic() + "\n" +
                            "Description: " + staticInfo.getDescription() + "\n";
                    return info;
                }).toList();
            }

    // JSON decoding for LLM simple responses has been moved to SimpleAiResponseDecoder

//    public void send(String message) {
//        rabbitTemplate.convertAndSend(
//                RabbitMQConfig.EXCHANGE,
//                RabbitMQConfig.ROUTING_KEY,
//                message
//        );
//    }

}
