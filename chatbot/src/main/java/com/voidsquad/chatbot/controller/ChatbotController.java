package com.voidsquad.chatbot.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class ChatbotController {

    private static final Logger log = LogManager.getLogger(ChatbotController.class);
    private final SimpMessagingTemplate messaging;

    public ChatbotController(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    @MessageMapping("/echo")
    public void handleEchoMessage(@Payload Map<String, Object> msg) {
        log.info("websocket broadcast echo");
        messaging.convertAndSend("/broadcast/msg", "[reply] "+msg);
    }

    @MessageMapping("/message")
    public void handleChatMessage(@Payload Map<String, Object> msg){
        log.info("websocket private echo");
        messaging.convertAndSendToUser("cmd-client","/user/msg", "[private reply] "+msg);
    }

}
