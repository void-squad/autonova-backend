package com.autonova.progressmonitoring.messaging.mapper;

import com.autonova.progressmonitoring.enums.EventCategory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * Simple mapper that converts event category -> default message using EventCategory enum.
 * Enhanced to produce specific messages for change-request and quote events.
 */
@Component
public class DefaultEventMessageMapper implements EventMessageMapper {

    public DefaultEventMessageMapper() {
        // no-op constructor for Spring
    }

    @Override
    public String mapToMessage(String routingKey, JsonNode payloadJson) {
        // derive a timestamp for message; fallback to now
        String timeStr = null;
        if (payloadJson != null) {
            if (payloadJson.has("occurredAt")) timeStr = payloadJson.get("occurredAt").asText(null);
            else if (payloadJson.has("approvedAt")) timeStr = payloadJson.get("approvedAt").asText(null);
            else if (payloadJson.has("rejectedAt")) timeStr = payloadJson.get("rejectedAt").asText(null);
        }

        String when = tryFormatTime(timeStr).orElse("now");

        EventCategory category = EventCategory.resolve(routingKey, payloadJson);
        String verb = category.verb();

        // Specialized handling based on routing key
        if (routingKey != null) {
            String rkLower = routingKey.toLowerCase();
            if (rkLower.startsWith("project.change-request")) {
                String id = extractId(payloadJson, "changeRequestId");
                if (id != null) {
                    return String.format("Change Request %s %s (%s)", shorten(id), verb, when);
                } else {
                    return String.format("Change Request %s (%s)", verb, when);
                }
            }
            if (rkLower.startsWith("quote.")) {
                String id = extractId(payloadJson, "quoteId");
                if (id != null) {
                    return String.format("Quote %s %s (%s)", shorten(id), verb, when);
                } else {
                    return String.format("Quote %s (%s)", verb, when);
                }
            }
        }

        // Project/title shortcuts
        String title = null;
        if (payloadJson != null) {
            if (payloadJson.has("title")) title = payloadJson.get("title").asText(null);
            else if (payloadJson.has("quoteId")) title = "Quote " + payloadJson.get("quoteId").asText();
        }

        if (title != null) {
            return String.format("%s %s (%s)", title, verb, when);
        } else {
            return String.format("Project %s (%s)", verb, when);
        }
    }

    private static Optional<String> tryFormatTime(String timeStr) {
        if (timeStr == null) return Optional.empty();
        try {
            var odt = OffsetDateTime.parse(timeStr);
            return Optional.of(odt.toLocalDateTime().toString());
        } catch (DateTimeParseException ex) {
            return Optional.of(timeStr); // timeStr is known non-null here
        }
    }

    private static String extractId(JsonNode payloadJson, String field) {
        if (payloadJson != null && payloadJson.has(field)) {
            return payloadJson.get(field).asText(null);
        }
        return null;
    }

    private static String shorten(String id) {
        // shorten UUID for display (first 8 chars) else return as-is
        if (id == null) return null;
        if (id.length() >= 8) return id.substring(0, 8);
        return id;
    }
}
