package com.autonova.notification.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationDto(
        UUID id,
        UUID userId,
        String type,
        String title,
        String message,
        Instant createdAt,
        boolean read
) {}

