package com.autonova.payments_billing_service.api.dto;

import java.util.List;

public record InvoiceListResponse(
    List<InvoiceResponse> items,
    long total,
    int limit,
    int offset
) {
}
