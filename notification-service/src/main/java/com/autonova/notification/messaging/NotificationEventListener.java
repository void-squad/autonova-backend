package com.autonova.notification.messaging;

import com.autonova.notification.domain.Notification;
import com.autonova.notification.service.NotificationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.amqp.support.AmqpHeaders.RECEIVED_ROUTING_KEY;

@Service
public class NotificationEventListener {
    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public NotificationEventListener(NotificationService notificationService, ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "${app.rabbit.queue}")
    public void handleEvent(String json, @Header(RECEIVED_ROUTING_KEY) String routingKey) {
        try {
            Map<String, Object> payload = objectMapper.readValue(json, new TypeReference<>() {});
            Optional<Notification> maybe = mapToNotification(routingKey, payload);
            maybe.ifPresent(notificationService::create);
        } catch (Exception ex) {
            log.error("Failed to process message with routingKey={}: {}", routingKey, ex.getMessage(), ex);
        }
    }

    private Optional<Notification> mapToNotification(String routingKey, Map<String, Object> payload) {
        if (routingKey == null) return Optional.empty();
        try {
            switch (routingKey) {
                case "project.created" -> {
                    UUID customerId = uuid(payload.get("customerId"));
                    if (customerId == null) return Optional.empty();
                    String title = "Project created";
                    String message = "Your project '" + str(payload.get("title")) + "' was created.";
                    return Optional.of(build(customerId, "project", title, message));
                }
                case "project.updated" -> {
                    UUID userId = uuid(payload.get("userId"));
                    if (userId == null) return Optional.empty();
                    String status = str(payload.get("status"));
                    String title = "Project updated";
                    String message = "Project status changed to " + status + ".";
                    return Optional.of(build(userId, "project", title, message));
                }
                case "quote.approved" -> {
                    UUID customerId = uuid(payload.get("customerId"));
                    if (customerId == null) return Optional.empty();
                    String title = "Quote approved";
                    String message = "Your quote was approved.";
                    return Optional.of(build(customerId, "quote", title, message));
                }
                case "quote.rejected" -> {
                    UUID customerId = uuid(payload.get("customerId"));
                    if (customerId == null) return Optional.empty();
                    String title = "Quote rejected";
                    String message = "Your quote was rejected.";
                    return Optional.of(build(customerId, "quote", title, message));
                }
                default -> {
                    UUID userId = uuid(payload.getOrDefault("userId", null));
                    if (userId != null) {
                        String title = "Event: " + routingKey;
                        String message = payload.toString();
                        return Optional.of(build(userId, "event", title, message));
                    }
                    return Optional.empty();
                }
            }
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private Notification build(UUID userId, String type, String title, String message) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        return n;
    }

    private static String str(Object o) { return o == null ? null : String.valueOf(o); }
    private static UUID uuid(Object o) { try { return o == null ? null : UUID.fromString(String.valueOf(o)); } catch (Exception e) { return null; } }
}

