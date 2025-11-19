package com.autonova.notification.service;

import com.autonova.notification.domain.Notification;
import com.autonova.notification.dto.NotificationDto;
import com.autonova.notification.repo.NotificationRepository;
import com.autonova.notification.sse.SseHub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private SseHub sseHub;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private Notification testNotification;
    private UUID notificationId;

    @BeforeEach
    void setUp() {
        notificationId = UUID.randomUUID();
        testNotification = createNotification(notificationId, 1L, "Test notification", false);
    }

    @Test
    void latestForUser_returnsNotifications_whenUserHasNotifications() {
        // Given
        Long userId = 1L;
        List<Notification> notifications = Arrays.asList(
                createNotification(UUID.randomUUID(), userId, "Notification 1", false),
                createNotification(UUID.randomUUID(), userId, "Notification 2", true)
        );
        when(notificationRepository.findTop50ByUserIdOrderByCreatedAtDesc(userId)).thenReturn(notifications);

        // When
        List<NotificationDto> result = notificationService.latestForUser(userId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).message()).isEqualTo("Notification 1");
        assertThat(result.get(0).read()).isFalse();
        assertThat(result.get(1).message()).isEqualTo("Notification 2");
        assertThat(result.get(1).read()).isTrue();
        verify(notificationRepository).findTop50ByUserIdOrderByCreatedAtDesc(userId);
    }

    @Test
    void latestForUser_returnsEmptyList_whenUserHasNoNotifications() {
        // Given
        Long userId = 1L;
        when(notificationRepository.findTop50ByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Collections.emptyList());

        // When
        List<NotificationDto> result = notificationService.latestForUser(userId);

        // Then
        assertThat(result).isEmpty();
        verify(notificationRepository).findTop50ByUserIdOrderByCreatedAtDesc(userId);
    }

    @Test
    void markRead_marksNotificationAsRead_whenNotificationExists() {
        // Given
        UUID notificationId = UUID.randomUUID();
        Notification notification = createNotification(notificationId, 1L, "Test", false);
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        notificationService.markRead(notificationId);

        // Then
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().isReadFlag()).isTrue();
        verify(sseHub).publish(any(NotificationDto.class));
    }

    @Test
    void markRead_doesNothing_whenNotificationNotFound() {
        // Given
        UUID notificationId = UUID.randomUUID();
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        // When
        notificationService.markRead(notificationId);

        // Then
        verify(notificationRepository, never()).save(any(Notification.class));
        verify(sseHub, never()).publish(any(NotificationDto.class));
    }

    @Test
    void create_savesNotification_whenNotDuplicate() {
        // Given
        Notification notification = createNotification(null, 1L, "New notification", false);
        notification.setMessageId("msg-123");
        when(notificationRepository.existsByMessageIdAndUserId("msg-123", 1L)).thenReturn(false);
        when(notificationRepository.save(notification)).thenReturn(notification);

        // When
        notificationService.create(notification);

        // Then
        verify(notificationRepository).save(notification);
        verify(sseHub).publish(any(NotificationDto.class));
    }

    @Test
    void create_skipsNotification_whenDuplicate() {
        // Given
        Notification notification = createNotification(null, 1L, "Duplicate notification", false);
        notification.setMessageId("msg-123");
        when(notificationRepository.existsByMessageIdAndUserId("msg-123", 1L)).thenReturn(true);

        // When
        notificationService.create(notification);

        // Then
        verify(notificationRepository, never()).save(any(Notification.class));
        verify(sseHub, never()).publish(any(NotificationDto.class));
    }

    @Test
    void createAll_savesMultipleNotifications() {
        // Given
        Notification notification1 = createNotification(null, 1L, "Notification 1", false);
        Notification notification2 = createNotification(null, 1L, "Notification 2", false);
        notification1.setMessageId("msg-1");
        notification2.setMessageId("msg-2");
        
        when(notificationRepository.existsByMessageIdAndUserId(anyString(), anyLong())).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        notificationService.createAll(Arrays.asList(notification1, notification2));

        // Then
        verify(notificationRepository, times(2)).save(any(Notification.class));
        verify(sseHub, times(2)).publish(any(NotificationDto.class));
    }

    @Test
    void unreadCount_returnsCount_whenUserHasUnreadNotifications() {
        // Given
        Long userId = 1L;
        when(notificationRepository.countByUserIdAndReadFlagFalse(userId)).thenReturn(5L);

        // When
        long result = notificationService.unreadCount(userId);

        // Then
        assertThat(result).isEqualTo(5L);
        verify(notificationRepository).countByUserIdAndReadFlagFalse(userId);
    }

    @Test
    void unreadCount_returnsZero_whenUserHasNoUnreadNotifications() {
        // Given
        Long userId = 1L;
        when(notificationRepository.countByUserIdAndReadFlagFalse(userId)).thenReturn(0L);

        // When
        long result = notificationService.unreadCount(userId);

        // Then
        assertThat(result).isEqualTo(0L);
        verify(notificationRepository).countByUserIdAndReadFlagFalse(userId);
    }

    @Test
    void markAllRead_marksAllNotificationsAsRead() {
        // Given
        Long userId = 1L;

        // When
        notificationService.markAllRead(userId);

        // Then
        verify(notificationRepository).markAllReadByUserId(userId);
    }

    private Notification createNotification(UUID id, Long userId, String message, boolean read) {
        Notification notification = new Notification();
        notification.setId(id);
        notification.setUserId(userId);
        notification.setType("info");
        notification.setEventType("general");
        notification.setTitle("Test Title");
        notification.setMessage(message);
        notification.setCreatedAt(Instant.now());
        notification.setReadFlag(read);
        return notification;
    }
}
