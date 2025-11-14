package com.autonova.payments_billing_service.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentEntityTest {

    @Test
    void onCreateUsesDefaultCurrencyWhenBlank() {
        PaymentEntity entity = new PaymentEntity();
        entity.setId(UUID.randomUUID());
        entity.setInvoice(new InvoiceEntity());
        entity.setCurrency("   ");
        entity.setAmount(1000L);

        entity.onCreate();

        assertThat(entity.getCurrency()).isEqualTo("lkr");
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isEqualTo(entity.getCreatedAt());
    }

    @Test
    void onUpdateRefreshesTimestampAndNormalizesCurrency() {
        PaymentEntity entity = new PaymentEntity();
        entity.setId(UUID.randomUUID());
        entity.setInvoice(new InvoiceEntity());
        entity.setCurrency(" Eur ");
        OffsetDateTime created = OffsetDateTime.now().minusDays(1);
        OffsetDateTime initialUpdated = created.minusHours(1);
        entity.setCreatedAt(created);
        entity.setUpdatedAt(initialUpdated);

        entity.onUpdate();

        assertThat(entity.getCurrency()).isEqualTo("eur");
        assertThat(entity.getUpdatedAt()).isAfter(initialUpdated);
        assertThat(entity.getCreatedAt()).isEqualTo(created);
    }
}
