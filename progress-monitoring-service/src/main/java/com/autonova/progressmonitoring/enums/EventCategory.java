package com.autonova.progressmonitoring.enums;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Locale;
import java.util.Objects;

/**
 * Known event categories with metadata for routing-key detection and message formatting.
 */
public enum EventCategory {
    CREATED("created", "created"),
    APPROVED("approved", "approved"),
    REJECTED("rejected", "rejected"),
    COMPLETED("completed", "completed"),
    UPDATED("updated", "updated"),
    APPLIED("applied", "applied"); // e.g. change-request applied

    private final String category;
    private final String verb;

    EventCategory(String category, String verb) {
        this.category = category;
        this.verb = verb;
    }

    public String category() { return category; }
    public String verb() { return verb; }

    /**
     * Resolve a category from routing key or payload status.
     * Priority: payload.status (if present) -> routingKey patterns -> UPDATED fallback
     */
    public static EventCategory resolve(String routingKey, JsonNode payload) {
        // 1) status field in payload
        if (payload != null && payload.has("status")) {
            var s = payload.get("status").asText(null);
            if (s != null && !s.isBlank()) {
                var name = s.trim().toLowerCase(Locale.ROOT);
                for (EventCategory c : values()) {
                    if (Objects.equals(c.category, name) || c.name().equalsIgnoreCase(name)) return c;
                }
            }
        }

        // 2) routing key heuristic
        if (routingKey != null && !routingKey.isBlank()) {
            String rk = routingKey.toLowerCase(Locale.ROOT);
            if (rk.contains(".created") || rk.contains("created")) return CREATED;
            if (rk.contains(".approved") || rk.contains("approved")) return APPROVED;
            if (rk.contains(".rejected") || rk.contains("rejected")) return REJECTED;
            if (rk.contains(".completed") || rk.contains("completed")) return COMPLETED;
            if (rk.contains(".applied") || rk.contains("applied")) return APPLIED;
            if (rk.contains(".updated") || rk.contains("updated")) return UPDATED;
        }

        return UPDATED;
    }
}
