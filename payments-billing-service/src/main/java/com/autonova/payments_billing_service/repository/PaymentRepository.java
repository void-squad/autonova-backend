package com.autonova.payments_billing_service.repository;

import com.autonova.payments_billing_service.domain.PaymentEntity;
import com.autonova.payments_billing_service.domain.PaymentStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {
    Optional<PaymentEntity> findByStripePaymentIntentId(String paymentIntentId);

    Optional<PaymentEntity> findFirstByInvoice_IdAndStatusOrderByCreatedAtDesc(UUID invoiceId, PaymentStatus status);
}
