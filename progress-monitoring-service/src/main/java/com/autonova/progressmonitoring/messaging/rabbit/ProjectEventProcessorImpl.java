package com.autonova.progressmonitoring.messaging.rabbit;

import com.autonova.progressmonitoring.enums.EventCategory;
import com.autonova.progressmonitoring.messaging.mapper.EventMessageMapper;
import com.autonova.progressmonitoring.messaging.publisher.EventPublisher;
import com.autonova.progressmonitoring.service.ProjectMessageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProjectEventProcessorImpl implements ProjectEventProcessor {
    private static final Logger log = LoggerFactory.getLogger(ProjectEventProcessorImpl.class);

    private final EventPublisher publisher;
    private final ObjectMapper mapper;
    private final EventMessageMapper messageMapper;
    private final ProjectMessageService messageService;

    public ProjectEventProcessorImpl(EventPublisher publisher, ObjectMapper mapper, EventMessageMapper messageMapper, ProjectMessageService messageService) {
        this.publisher = publisher;
        this.mapper = mapper;
        this.messageMapper = messageMapper;
        this.messageService = messageService;
    }

    @Override
    public void process(Message message) {
        String routingKey = Optional.ofNullable(message.getMessageProperties().getReceivedRoutingKey()).orElse("");
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        log.debug("Processing message (routingKey={}): {}", routingKey, body);

        try {
            JsonNode node = mapper.readTree(body);
            String messageText = messageMapper.mapToMessage(routingKey, node);

            if (node.has("projectId")) {
                var projectIdText = node.get("projectId").asText();
                publisher.publishToProject(projectIdText, body);
                publisher.publishMessageToProject(projectIdText, messageText);

                // persist message
                try {
                    UUID projectId = UUID.fromString(projectIdText);
                    OffsetDateTime occurredAt = null;
                    if (node.has("occurredAt")) {
                        try {
                            occurredAt = OffsetDateTime.parse(node.get("occurredAt").asText());
                        } catch (DateTimeParseException ex) {
                            log.debug("Could not parse occurredAt timestamp, proceeding with null value", ex);
                        }
                    }
                    // derive category using EventCategory enum for consistency
                    String category = EventCategory.resolve(routingKey, node).name();
                    messageService.saveMessage(projectId, category, messageText, body, occurredAt);
                } catch (Exception ex) {
                    log.debug("Failed to persist project message for project {}: {}", projectIdText, ex.getMessage(), ex);
                }

                return;
            }
        } catch (Exception ex) {
            log.debug("Failed to parse message body as JSON to extract projectId", ex);
        }

        // fallback: broadcast raw payload and a generic message
        String genericMessage = messageMapper.mapToMessage(routingKey, null);
        publisher.broadcast(body);
        publisher.broadcastMessage(genericMessage);
    }
}
