package com.autonova.payments_billing_service.repository;

import com.autonova.payments_billing_service.domain.InvoiceEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface InvoiceRepository extends JpaRepository<InvoiceEntity, UUID>, JpaSpecificationExecutor<InvoiceEntity> {
    Optional<InvoiceEntity> findByProjectId(UUID projectId);
}
