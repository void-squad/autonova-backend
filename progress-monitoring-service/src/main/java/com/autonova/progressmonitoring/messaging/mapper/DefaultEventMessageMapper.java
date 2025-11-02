package com.autonova.progressmonitoring.messaging.mapper;

import com.autonova.progressmonitoring.enums.EventCategory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * Simple mapper that converts event category -> default message using EventCategory enum.
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

        // Project/title shortcuts
        String title = null;
        if (payloadJson != null) {
            if (payloadJson.has("title")) title = payloadJson.get("title").asText(null);
            else if (payloadJson.has("quoteId")) title = "Quote " + payloadJson.get("quoteId").asText();
        }

        String verb = category.verb();
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
            return Optional.ofNullable(timeStr);
        }
    }
}
