package com.autonova.progressmonitoring.messaging;

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

    public ProjectEventProcessorImpl(EventPublisher publisher, ObjectMapper mapper) {
        this.publisher = publisher;
        this.mapper = mapper;
    }

    @Override
    public void process(Message message) {
        String routingKey = Optional.ofNullable(message.getMessageProperties().getReceivedRoutingKey()).orElse("");
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        log.debug("Processing message (routingKey={}): {}", routingKey, body);

        try {
            JsonNode node = mapper.readTree(body);
            if (node.has("projectId")) {
                var projectId = node.get("projectId").asText();
                publisher.publishToProject(projectId, body);
                return;
            }
        } catch (Exception ex) {
            log.debug("Failed to parse message body as JSON to extract projectId", ex);
        }

        publisher.broadcast(body);
    }
}
