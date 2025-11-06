package com.voidsquad.chatbot.controller;

import com.voidsquad.chatbot.service.AIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.repository.query.Param;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChatbotController {

    private static final Logger log = LoggerFactory.getLogger(ChatbotController.class);
    private final SimpMessagingTemplate messaging;
    private final AIService aiService;

    public ChatbotController(SimpMessagingTemplate messaging, AIService aiService){
        this.aiService = aiService;
        this.messaging = messaging;
    }

    @Profile("dev")
    @GetMapping("/v1/hello")
    public String hello(){
        return "hello!";
    }

    @Profile("dev")
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


    @GetMapping("/v1/ai")
    public String answerWithAI(@Param("prompt") String prompt){
        return aiService.generation(prompt);
    }


}
