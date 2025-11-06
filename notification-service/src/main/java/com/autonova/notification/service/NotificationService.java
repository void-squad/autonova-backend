package com.autonova.notification.service;

import com.autonova.notification.domain.Notification;
import com.autonova.notification.dto.NotificationDto;
import java.util.List;
import java.util.UUID;
import reactor.core.publisher.Flux;

public interface NotificationService {
    Flux<NotificationDto> streamForUser(UUID userId);
    List<NotificationDto> latestForUser(UUID userId);
    void markRead(UUID notificationId);
    void create(Notification notification);
}
