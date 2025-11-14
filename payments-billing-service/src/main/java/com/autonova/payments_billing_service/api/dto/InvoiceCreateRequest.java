package com.autonova.payments_billing_service.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record InvoiceCreateRequest(
    @NotNull UUID projectId,
    UUID quoteId,
    @NotBlank @Size(max = 255) String projectName,
    @Size(max = 2000) String projectDescription,
    @Positive long amountTotal,
    @NotBlank @Size(min = 3, max = 3) String currency
) {}
