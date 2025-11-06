package com.autonova.notification.service;

import com.autonova.notification.domain.Notification;
import com.autonova.notification.dto.NotificationDto;
import com.autonova.notification.repo.NotificationRepository;
import com.autonova.notification.sse.SseHub;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final SseHub sseHub;

    public NotificationServiceImpl(NotificationRepository notificationRepository, SseHub sseHub) {
        this.notificationRepository = notificationRepository;
        this.sseHub = sseHub;
    }

    @Override
    public Flux<NotificationDto> streamForUser(UUID userId) {
        // Stub: In a real implementation, this would use a reactive source (e.g., SSE, messaging)
        return Flux.fromIterable(latestForUser(userId));
    }

    @Override
    public List<NotificationDto> latestForUser(UUID userId) {
        List<Notification> notifications = notificationRepository.findTop50ByUserIdOrderByCreatedAtDesc(userId);
        return notifications.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public void markRead(UUID notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setReadFlag(true);
            Notification saved = notificationRepository.save(notification);
            // Optionally notify clients about the change
            sseHub.publish(toDto(saved));
        });
    }

    @Override
    public void create(Notification notification) {
        Notification saved = notificationRepository.save(notification);
        sseHub.publish(toDto(saved));
    }

    private NotificationDto toDto(Notification n) {
        return new NotificationDto(
                n.getId(),
                n.getUserId(),
                n.getType(),
                n.getTitle(),
                n.getMessage(),
                n.getCreatedAt(),
                n.isReadFlag()
        );
    }
}
