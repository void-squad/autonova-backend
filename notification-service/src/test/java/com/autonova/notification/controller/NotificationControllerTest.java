package com.autonova.notification.controller;

import com.autonova.notification.dto.NotificationDto;
import com.autonova.notification.service.NotificationService;
import com.autonova.notification.sse.SseHub;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.Mockito.*;

@WebFluxTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private SseHub sseHub;

    @Test
    void latest_returnsNotifications_whenUserHasNotifications() {
        // Given
        Long userId = 1L;
        NotificationDto notification1 = createNotificationDto(UUID.randomUUID(), userId, "Message 1", false);
        NotificationDto notification2 = createNotificationDto(UUID.randomUUID(), userId, "Message 2", true);
        
        when(notificationService.latestForUser(userId)).thenReturn(Arrays.asList(notification1, notification2));

        // When & Then
        webTestClient.get()
                .uri("/api/notifications/{userId}", userId)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(NotificationDto.class)
                .hasSize(2)
                .contains(notification1, notification2);

        verify(notificationService).latestForUser(userId);
    }

    @Test
    void latest_returnsEmptyArray_whenUserHasNoNotifications() {
        // Given
        Long userId = 1L;
        when(notificationService.latestForUser(userId)).thenReturn(Collections.emptyList());

        // When & Then
        webTestClient.get()
                .uri("/api/notifications/{userId}", userId)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(NotificationDto.class)
                .hasSize(0);

        verify(notificationService).latestForUser(userId);
    }

    @Test
    void markRead_returnsNoContent_whenNotificationIsMarked() {
        // Given
        UUID notificationId = UUID.randomUUID();
        doNothing().when(notificationService).markRead(notificationId);

        // When & Then
        webTestClient.post()
                .uri("/api/notifications/{id}/read", notificationId)
                .exchange()
                .expectStatus().isNoContent();

        verify(notificationService).markRead(notificationId);
    }

    @Test
    void unreadCount_returnsCount_whenUserHasUnreadNotifications() {
        // Given
        Long userId = 1L;
        when(notificationService.unreadCount(userId)).thenReturn(5L);

        // When & Then
        webTestClient.get()
                .uri("/api/notifications/{userId}/unread-count", userId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Long.class)
                .isEqualTo(5L);

        verify(notificationService).unreadCount(userId);
    }

    @Test
    void unreadCount_returnsZero_whenUserHasNoUnreadNotifications() {
        // Given
        Long userId = 1L;
        when(notificationService.unreadCount(userId)).thenReturn(0L);

        // When & Then
        webTestClient.get()
                .uri("/api/notifications/{userId}/unread-count", userId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Long.class)
                .isEqualTo(0L);

        verify(notificationService).unreadCount(userId);
    }

    @Test
    void markAllRead_returnsNoContent_whenAllNotificationsAreMarked() {
        // Given
        Long userId = 1L;
        doNothing().when(notificationService).markAllRead(userId);

        // When & Then
        webTestClient.post()
                .uri("/api/notifications/{userId}/read-all", userId)
                .exchange()
                .expectStatus().isNoContent();

        verify(notificationService).markAllRead(userId);
    }

    @Test
    void stream_returnsFlux_whenUserSubscribes() {
        // Given
        Long userId = 1L;
        NotificationDto notification = createNotificationDto(UUID.randomUUID(), userId, "Test notification", false);
        when(sseHub.subscribe(userId)).thenReturn(Flux.just(notification));

        // When & Then
        webTestClient.get()
                .uri("/api/notifications/stream/{userId}", userId)
                .exchange()
                .expectStatus().isOk();

        verify(sseHub).subscribe(userId);
    }

    private NotificationDto createNotificationDto(UUID id, Long userId, String message, boolean read) {
        return new NotificationDto(
                id,
                userId,
                "info",
                "general",
                "Test Title",
                message,
                "msg-" + id,
                Instant.now(),
                read
        );
    }
}
