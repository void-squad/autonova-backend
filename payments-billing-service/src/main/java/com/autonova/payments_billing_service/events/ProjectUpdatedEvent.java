package com.autonova.payments_billing_service.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProjectUpdatedEvent(
    UUID id,
    String type,
    @JsonProperty("occurred_at")
    OffsetDateTime occurredAt,
    int version,
    ProjectUpdatedData data
) {

    public record ProjectUpdatedData(
        @JsonProperty("project_id")
        UUID projectId,
        @JsonProperty("status")
        String status
    ) {
    }
}
