package com.autonova.notification.repo;

import com.autonova.notification.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findTop50ByUserIdOrderByCreatedAtDesc(UUID userId);
    long countByUserIdAndReadFlagFalse(UUID userId);
    boolean existsByMessageIdAndUserId(String messageId, UUID userId);

    @Modifying
    @Query("update Notification n set n.readFlag = true where n.userId = :userId and n.readFlag = false")
    int markAllReadByUserId(@Param("userId") UUID userId);
}
