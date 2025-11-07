package com.autonova.payments_billing_service.api.dto;

import com.autonova.payments_billing_service.domain.InvoiceEntity;
import com.autonova.payments_billing_service.domain.InvoiceStatus;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

public record InvoiceResponse(
    UUID id,
    UUID projectId,
    UUID quoteId,
    String projectName,
    String projectDescription,
    String customerEmail,
    Long customerUserId,
    String currency,
    long amountTotal,
    String paymentMethod,
    InvoiceStatus status,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {

    public static InvoiceResponse fromEntity(InvoiceEntity entity) {
        return fromEntity(entity, null);
    }

    public static InvoiceResponse fromEntity(InvoiceEntity entity, String paymentMethod) {
        String normalizedCurrency = entity.getCurrency() != null
            ? entity.getCurrency().toUpperCase(Locale.ROOT)
            : null;
        return new InvoiceResponse(
            entity.getId(),
            entity.getProjectId(),
            entity.getQuoteId(),
            entity.getProjectName(),
            entity.getProjectDescription(),
            entity.getCustomerEmail(),
            entity.getCustomerUserId(),
            normalizedCurrency,
            entity.getAmountTotal(),
            paymentMethod,
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
