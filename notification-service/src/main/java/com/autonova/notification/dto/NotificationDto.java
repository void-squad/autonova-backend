package com.autonova.notification.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationDto(
        UUID id,
        Long userId,
        String type,
        String eventType,
        String title,
        String message,
        String messageId,
        Instant createdAt,
        boolean read
) {}
