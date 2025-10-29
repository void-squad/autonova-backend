package com.autonova.payments_billing_service.api.dto;

import com.autonova.payments_billing_service.domain.InvoiceEntity;
import com.autonova.payments_billing_service.domain.InvoiceStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record InvoiceResponse(
    UUID id,
    UUID projectId,
    UUID customerId,
    UUID quoteId,
    String currency,
    long amountTotal,
    InvoiceStatus status,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {

    public static InvoiceResponse fromEntity(InvoiceEntity entity) {
        return new InvoiceResponse(
            entity.getId(),
            entity.getProjectId(),
            entity.getCustomerId(),
            entity.getQuoteId(),
            entity.getCurrency(),
            entity.getAmountTotal(),
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
