package com.autonova.payments_billing_service.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.autonova.payments_billing_service.domain.InvoiceStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InvoiceFilterTest {

    @Test
    void hasSearchTermReturnsTrueForNonBlankInput() {
        InvoiceFilter filter = new InvoiceFilter(InvoiceStatus.OPEN, UUID.randomUUID(), "  Engine Overhaul  ");

        assertThat(filter.hasSearchTerm()).isTrue();
    }

    @Test
    void hasSearchTermReturnsFalseForNullOrBlank() {
        InvoiceFilter nullFilter = new InvoiceFilter(null, null, null);
        InvoiceFilter blankFilter = new InvoiceFilter(null, null, "   ");

        assertThat(nullFilter.hasSearchTerm()).isFalse();
        assertThat(blankFilter.hasSearchTerm()).isFalse();
    }
}
