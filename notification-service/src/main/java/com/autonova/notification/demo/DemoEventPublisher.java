package com.autonova.notification.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "app.demo", name = "enabled", havingValue = "true")
public class DemoEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DemoEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.rabbit.exchange:autonova.events}")
    private String exchange;

    public DemoEventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishAppointmentCreated(UUID userId) {
        String eventType = "appointment.created";
        Map<String, Object> data = new HashMap<>();
        data.put("appointment_id", UUID.randomUUID().toString());
        data.put("customer_id", userId.toString());
        data.put("vehicle_id", UUID.randomUUID().toString());
        data.put("service_type", "MAINTENANCE");
        data.put("scheduled_at", "2025-12-01T10:00:00Z");
        data.put("status", "PENDING");
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", eventType);
        payload.put("version", 1);
        payload.put("data", data);
        sendJson(payload, eventType, userId);
    }

    public void publishProjectApproved(UUID userId) {
        String eventType = "project.approved";
        Map<String, Object> data = new HashMap<>();
        data.put("project_id", UUID.randomUUID().toString());
        data.put("customer_id", userId.toString());
        data.put("status", "APPROVED");
        data.put("start_date", "2025-12-05");
        data.put("end_date", "2025-12-20");
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", eventType);
        payload.put("version", 1);
        payload.put("data", data);
        sendJson(payload, eventType, userId);
    }

    public void publishPaymentSucceeded(UUID userId) {
        String eventType = "payment.succeeded";
        Map<String, Object> data = new HashMap<>();
        data.put("payment_id", UUID.randomUUID().toString());
        data.put("invoice_id", UUID.randomUUID().toString());
        data.put("project_id", UUID.randomUUID().toString());
        data.put("amount", 19900);
        data.put("currency", "USD");
        data.put("provider", "STRIPE");
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", eventType);
        payload.put("version", 1);
        payload.put("data", data);
        sendJson(payload, eventType, userId);
    }

    private void sendJson(Map<String, Object> payload, String eventType, UUID userId) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            String msgId = UUID.randomUUID().toString();
            MessageProperties props = new MessageProperties();
            props.setContentType("application/json");
            props.setMessageId(msgId);
            props.setHeader("x-event-name", eventType);
            props.setHeader("x-event-version", 1);
            props.setHeader("x-recipients-user-ids", userId.toString());
            Message message = new Message(json.getBytes(StandardCharsets.UTF_8), props);
            rabbitTemplate.send(exchange, eventType, message);
            log.info("[DEMO] Published {} with messageId={} to exchange={} for user {}", eventType, msgId, exchange, userId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish demo event " + eventType, e);
        }
    }
}
