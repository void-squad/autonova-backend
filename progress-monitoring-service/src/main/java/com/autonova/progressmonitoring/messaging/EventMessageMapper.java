package com.autonova.progressmonitoring.messaging;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Contract for mapping incoming events to user-friendly messages.
 */
public interface EventMessageMapper {
    /**
     * Map an event (routing key + parsed JSON) to a short human-friendly message.
     */
    String mapToMessage(String routingKey, JsonNode payloadJson);
}
