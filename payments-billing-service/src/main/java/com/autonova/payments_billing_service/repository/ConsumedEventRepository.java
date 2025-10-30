package com.autonova.payments_billing_service.repository;

import com.autonova.payments_billing_service.domain.ConsumedEventEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsumedEventRepository extends JpaRepository<ConsumedEventEntity, UUID> {
}
