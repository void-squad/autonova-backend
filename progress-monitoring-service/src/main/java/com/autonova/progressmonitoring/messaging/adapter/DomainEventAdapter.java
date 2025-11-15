package com.autonova.progressmonitoring.messaging.adapter;

import java.time.OffsetDateTime;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

public interface DomainEventAdapter {
    AdaptedEvent adapt(String routingKey, JsonNode payload);

    record AdaptedEvent(String routingKey,
                        String category,
                        String projectId,
                        String friendlyMessage,
                        String rawPayload,
                        OffsetDateTime occurredAt) {}

    static Optional<OffsetDateTime> parseTimestamp(JsonNode node) {
        if (node == null) return Optional.empty();
        if (node.has("occurredAt")) {
            try {
                return Optional.of(OffsetDateTime.parse(node.get("occurredAt").asText()));
            } catch (Exception ignored) {

            }
        }
        return Optional.empty();
    }
}

