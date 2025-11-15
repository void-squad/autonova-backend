package com.voidsquad.chatbot.controller;

import com.voidsquad.chatbot.entities.WorkflowStep;
import com.voidsquad.chatbot.repository.WorkflowStepRepository;
import com.voidsquad.chatbot.service.AIService;
import com.voidsquad.chatbot.service.embedding.EmbeddingService;
import com.voidsquad.chatbot.service.workflow.WorkflowStepService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.repository.query.Param;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class ChatbotController {

    private static final Logger log = LoggerFactory.getLogger(ChatbotController.class);
    private final SimpMessagingTemplate messaging;
    private final AIService aiService;
    private final WorkflowStepService workflowStepService;


    public ChatbotController(SimpMessagingTemplate messaging, AIService aiService, WorkflowStepRepository workflowStepRepository, EmbeddingService embeddingService, WorkflowStepService workflowStepService){
        this.aiService = aiService;
        this.messaging = messaging;
        this.workflowStepService = workflowStepService;
    }

    @GetMapping("/hello")
    public String hello(){
        return "hello!";
    }

    @MessageMapping("/echo")
    public void handleEchoMessage(@Payload Map<String, Object> msg) {
        log.info("websocket broadcast echo");
        messaging.convertAndSend("/broadcast/msg", "[reply] "+msg);
    }

    @MessageMapping("/message")
    public void handleChatMessage(@Payload Map<String, Object> msg,
                                  Principal principal,
                                  @Header("simpSessionId") String sessionId){
        log.info("websocket private echo");
        log.info("sessionId "+sessionId);
        Object senderObj = msg.get("sender");
        Object tokenObj = msg.get("token");
        String sender = (senderObj != null) ? senderObj.toString() : null;
        String token = (tokenObj != null) ? tokenObj.toString() : null;
        if (sender == null || sender.isEmpty()) {
            log.warn("No sender provided in message payload; cannot send private reply");
            return;
        }
        if( token == null || token.isEmpty() ){
            log.warn("No token provided for sender");
            return;
        }
        //TODO: validate auth token
        //TODO: generate replay from message
        String dest = "/queue/user/" + sender;
        log.info("destination user "+ dest);
        messaging.convertAndSend(dest, "[private reply] "+msg);
    }


    @Profile("default")
    @GetMapping("/lm")
    public String answerWithAI(@Param("prompt") String prompt){
        return aiService.generation(prompt);
    }

    @Profile("default")
    @GetMapping("/send")
    public String sendMessage(@Param("msg") String msg) {
//        aiService.send(msg);
        return "msg sent";
    }

    @GetMapping("ai")
    public String simpleAIResponse(@Param("prompt") String prompt){
        try {
            return aiService.requestHandler(prompt);
        }catch (Exception e){
            log.error("Error in AI response: " + e.getMessage());
            return "Error processing request";
        }
    }

    @Profile("default")
    @GetMapping("/workflowSteps")
    public Iterable<WorkflowStep> getAllWorkflowSteps(
            @Param("keyword") String keyword
    ) {
        return workflowStepService.findSimilarSteps(keyword,10);
    }

    @Profile("default")
    @GetMapping("/staticInfo")
    public List<String> getStaticInfo(
            @Param("query") String query
    ) {
        return aiService.getAllStaticInfoByEmbeddings(query);
    }

    @Profile("default")
    @PostMapping("/workflowStep")
    public String test(
            @RequestParam("WorkflowName") String workflowName,
            @RequestParam("WorkflowDescription") String workflowDescription
                       ) {
        log.info("Creating workflow step: " + workflowName);
        log.info("Description: " + workflowDescription);
        WorkflowStep workflowStep = new WorkflowStep();
        workflowStep.setName(workflowName);
        workflowStep.setDescription(workflowDescription);
        workflowStepService.saveWorkflowStep(workflowStep);
        log.info("Workflow step saved with ID: " + workflowStep.getId());
        return "Saved";
    }

    @Profile("default")
    @PostMapping("/staticInfo" )
    public String addStaticInfo(
            @RequestParam("topic") String topic,
            @RequestParam("description") String description
    ) {
        log.info("Adding static info: " + topic);
        aiService.addStaticInfo(topic, description);
        return "Static info added";
    }

    @Profile("default")
    @PostMapping("/staticInfo/bulk" )
    public String addBulkStaticInfo(
            @RequestParam("file") MultipartFile file
    ) {
        log.info("Adding bulk static info from file");
        try {
            var staticInfoList = aiService.ReadStaticInfoFromCSV(file);
            aiService.addBulkStaticInfo(staticInfoList);
            return "Bulk static info added: " + staticInfoList.size() + " entries.";
        } catch (Exception e) {
            log.error("Error adding bulk static info: " + e.getMessage());
            return "Error adding bulk static info: " + e.getMessage();
        }
    }
}
