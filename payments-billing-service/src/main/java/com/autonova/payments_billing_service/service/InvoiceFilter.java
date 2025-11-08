package com.autonova.payments_billing_service.service;

import com.autonova.payments_billing_service.domain.InvoiceStatus;
import java.util.UUID;

public record InvoiceFilter(InvoiceStatus status, UUID projectId, String searchTerm) {

    public boolean hasSearchTerm() {
        return searchTerm != null && !searchTerm.isBlank();
    }
}
