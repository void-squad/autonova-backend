package com.autonova.payments_billing_service.service;

import java.util.UUID;

public record CreateInvoiceCommand(
    UUID projectId,
    UUID quoteId,
    String projectName,
    String projectDescription,
    long amountTotal,
    String currency
) {}
