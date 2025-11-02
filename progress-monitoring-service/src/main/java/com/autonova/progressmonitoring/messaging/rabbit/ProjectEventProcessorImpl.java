package com.autonova.progressmonitoring.messaging.rabbit;

import com.autonova.progressmonitoring.messaging.EventMessageMapper;
import com.autonova.progressmonitoring.messaging.publisher.EventPublisher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
public class ProjectEventProcessorImpl implements ProjectEventProcessor {
    private static final Logger log = LoggerFactory.getLogger(ProjectEventProcessorImpl.class);

    private final EventPublisher publisher;
    private final ObjectMapper mapper;
    private final EventMessageMapper messageMapper;

    public ProjectEventProcessorImpl(EventPublisher publisher, ObjectMapper mapper, EventMessageMapper messageMapper) {
        this.publisher = publisher;
        this.mapper = mapper;
        this.messageMapper = messageMapper;
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
                var projectId = node.get("projectId").asText();
                publisher.publishToProject(projectId, body);
                publisher.publishMessageToProject(projectId, messageText);
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
