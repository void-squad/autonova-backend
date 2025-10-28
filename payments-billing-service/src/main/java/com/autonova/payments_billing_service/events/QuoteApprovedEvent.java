package com.autonova.payments_billing_service.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.UUID;

public record QuoteApprovedEvent(
    UUID id,
    String type,
    @JsonProperty("occurred_at")
    OffsetDateTime occurredAt,
    int version,
    QuoteApprovedData data
) {

    public record QuoteApprovedData(
        @JsonProperty("project_id")
        UUID projectId,
        @JsonProperty("customer_id")
        UUID customerId,
        @JsonProperty("quote_id")
        UUID quoteId,
        @JsonProperty("total")
        long total,
        @JsonProperty("currency")
        String currency,
        @JsonProperty("status")
        String status
    ) {
    }
}
