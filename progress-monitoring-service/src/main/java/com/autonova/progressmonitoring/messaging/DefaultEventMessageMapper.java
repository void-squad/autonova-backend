package com.autonova.progressmonitoring.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;

/**
 * Simple mapper that converts event category -> default message.
 * Reverted from the strategy-based approach to a straightforward mapping.
 */
@Component
public class DefaultEventMessageMapper implements EventMessageMapper {

    private static final Map<String, String> VERB_BY_CATEGORY = Map.ofEntries(
            Map.entry("created", "created"),
            Map.entry("approved", "approved"),
            Map.entry("rejected", "rejected"),
            Map.entry("completed", "completed"),
            Map.entry("updated", "updated"),
            Map.entry("update", "updated")
    );

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

        // Determine category from routing key or payload fields
        String category = "update";
        if (routingKey != null) {
            if (routingKey.endsWith(".created") || routingKey.contains(".created")) category = "created";
            else if (routingKey.endsWith(".updated") || routingKey.contains(".updated")) category = "updated";
            else if (routingKey.endsWith(".approved") || routingKey.contains(".approved")) category = "approved";
            else if (routingKey.endsWith(".rejected") || routingKey.contains(".rejected")) category = "rejected";
            else if (routingKey.endsWith(".completed") || routingKey.contains(".completed")) category = "completed";
        }

        // Some payloads carry a status field we can use
        if (payloadJson != null && payloadJson.has("status")) {
            var s = payloadJson.get("status").asText(null);
            if (s != null && !s.isBlank()) category = s.toLowerCase();
        }

        // Project/title shortcuts
        String title = null;
        if (payloadJson != null) {
            if (payloadJson.has("title")) title = payloadJson.get("title").asText(null);
            else if (payloadJson.has("quoteId")) title = "Quote " + payloadJson.get("quoteId").asText();
        }

        String verb = VERB_BY_CATEGORY.getOrDefault(category, "updated");
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
