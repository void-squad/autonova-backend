package com.autonova.progressmonitoring.messaging.adapter;

import com.autonova.progressmonitoring.enums.EventCategory;
import com.autonova.progressmonitoring.messaging.mapper.EventMessageMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class DefaultDomainEventAdapter implements DomainEventAdapter {

    private final EventMessageMapper messageMapper;

    public DefaultDomainEventAdapter(EventMessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    @Override
    public AdaptedEvent adapt(String routingKey, JsonNode payload) {
        String friendly = messageMapper.mapToMessage(routingKey, payload);
        EventCategory category = EventCategory.resolve(routingKey, payload);
        String projectId = payload != null && payload.has("projectId") ? payload.get("projectId").asText() : null;
        OffsetDateTime occurredAt = DomainEventAdapter.parseTimestamp(payload).orElse(null);
        return new AdaptedEvent(routingKey, category.name(), projectId, friendly, payload == null ? null : payload.toString(), occurredAt);
    }
}

