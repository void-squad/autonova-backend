package com.autonova.payments_billing_service.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.Locale;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "invoices")
public class InvoiceEntity {

    @Id
    private UUID id;

    @Column(name = "project_id", nullable = false, unique = true)
    private UUID projectId;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Column(name = "customer_user_id", nullable = false)
    private Long customerUserId;

    @Column(name = "quote_id")
    private UUID quoteId;

    @Column(name = "project_name")
    private String projectName;

    @Column(name = "project_description")
    private String projectDescription;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "lkr";

    @Column(name = "amount_total", nullable = false)
    private long amountTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private InvoiceStatus status = InvoiceStatus.OPEN;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        normalizeCurrency();
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        normalizeCurrency();
        updatedAt = OffsetDateTime.now();
    }

    @PostLoad
    void onLoad() {
        normalizeCurrency();
    }

    private void normalizeCurrency() {
        if (currency == null || currency.isBlank()) {
            currency = "lkr";
            return;
        }
        currency = currency.trim().toLowerCase(Locale.ROOT);
    }
}
