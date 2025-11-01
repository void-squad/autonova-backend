package com.voidsquad.chatbot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ChatbotControllerTest {
    @LocalServerPort
    private int port;

    private WebSocketStompClient stompClient;

    @BeforeEach
    void setup() {
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        // scheduler for heartbeats and internal tasks (avoids warnings in test)
        stompClient.setTaskScheduler(new ConcurrentTaskScheduler());
        stompClient.setDefaultHeartbeat(new long[]{10000, 10000});
    }

    @Test
    void testChatMessaging() throws Exception {
        StompSession session = stompClient.connectAsync(
                "ws://localhost:" + port + "/chat/websocket",
                new StompSessionHandlerAdapter() {}
        ).get(5, TimeUnit.SECONDS);

        try {
            CompletableFuture<Map> topicFuture = new CompletableFuture<>();

            session.subscribe("/topic/messages", new StompFrameHandler() {
                public Type getPayloadType(StompHeaders headers) {
                    return Map.class; // expect JSON object -> Map
                }
                public void handleFrame(StompHeaders headers, Object payload) {
                    topicFuture.complete((Map) payload);
                }
            });

            Map<String, String> message = Map.of("sender", "test", "content", "Hello!");
            session.send("/app/chat", message);

            Map topicMsg = topicFuture.get(5, TimeUnit.SECONDS);

            assertNotNull(topicMsg, "Did not receive broadcast on /topic/messages");
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }
}
