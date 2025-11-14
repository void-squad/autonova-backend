package com.autonova.payments_billing_service.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InvoiceEntityTest {

    @Test
    void onCreateInitializesTimestampsAndCurrencyDefaults() {
        InvoiceEntity entity = new InvoiceEntity();
        entity.setProjectId(UUID.randomUUID());
        entity.setCustomerEmail("customer@example.com");
        entity.setCustomerUserId(42L);
        entity.setCurrency(null);
        entity.setCreatedAt(null);
        entity.setUpdatedAt(null);

        entity.onCreate();

        assertThat(entity.getCurrency()).isEqualTo("lkr");
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isEqualTo(entity.getCreatedAt());
    }

    @Test
    void lifecycleMethodsNormalizeCurrency() {
        InvoiceEntity entity = new InvoiceEntity();
        entity.setProjectId(UUID.randomUUID());
        entity.setCustomerEmail("customer@example.com");
        entity.setCustomerUserId(42L);
        entity.setCurrency(" USD ");

        entity.onCreate();
        assertThat(entity.getCurrency()).isEqualTo("usd");

        entity.setCurrency(" Eur ");
        OffsetDateTime previousUpdated = entity.getUpdatedAt();

        entity.onUpdate();

        assertThat(entity.getCurrency()).isEqualTo("eur");
        assertThat(entity.getUpdatedAt()).isAfter(previousUpdated);
    }
}
