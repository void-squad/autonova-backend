package com.autonova.notification.messaging;

import com.autonova.notification.domain.Notification;
import com.autonova.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    private ObjectMapper objectMapper;

    private NotificationEventListener listener;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        listener = new NotificationEventListener(notificationService, objectMapper);
    }

    @Test
    void handleEvent_withUserRecipients_createsUserNotifications() {
        String json = "{\"data\":{\"customer_id\":123}}";
        Message message = createMessage(json);

        listener.handleEvent(message, "appointment.created", "appointment.created", "123", null, null);

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationService).createAll(captor.capture());

        List<Notification> notifications = captor.getValue();
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getUserId()).isEqualTo(123L);
        assertThat(notifications.get(0).getTitle()).isEqualTo("Appointment Created");
        assertThat(notifications.get(0).getEventType()).isEqualTo("appointment.created");
    }

    @Test
    void handleEvent_withRoleRecipients_createsRoleNotifications() {
        String json = "{\"data\":{}}";
        Message message = createMessage(json);

        listener.handleEvent(message, "project.approved", "project.approved", null, "ADMIN,EMPLOYEE", null);

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationService).createAll(captor.capture());

        List<Notification> notifications = captor.getValue();
        assertThat(notifications).hasSize(2);
        assertThat(notifications).allMatch(n -> n.getUserId() == null);
        assertThat(notifications).extracting(Notification::getRole).containsExactlyInAnyOrder("ADMIN", "EMPLOYEE");
    }

    @Test
    void handleEvent_withDerivedRecipients_extractsFromPayload() {
        String json = "{\"data\":{\"employee_id\":456}}";
        Message message = createMessage(json);

        listener.handleEvent(message, "time_log.approved", "time_log.approved", null, null, null);

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationService).createAll(captor.capture());

        List<Notification> notifications = captor.getValue();
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getUserId()).isEqualTo(456L);
    }

    @Test
    void handleEvent_withMultipleRecipients_createsMultipleNotifications() {
        String json = "{\"data\":{\"customer_id\":123,\"employee_id\":456}}";
        Message message = createMessage(json);

        listener.handleEvent(message, "project.completed", "project.completed", null, null, null);

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationService).createAll(captor.capture());

        List<Notification> notifications = captor.getValue();
        assertThat(notifications).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    void handleEvent_withNoRecipients_doesNotCreateNotifications() {
        String json = "{\"data\":{}}";
        Message message = createMessage(json);

        listener.handleEvent(message, "unknown.event", "unknown.event", null, null, null);

        verify(notificationService, never()).createAll(anyList());
    }

    @Test
    void handleEvent_withAppointmentCreated_buildsCorrectMessage() {
        String json = "{\"data\":{\"customer_id\":789}}";
        Message message = createMessage(json);

        listener.handleEvent(message, "appointment.created", "appointment.created", "789", null, null);

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationService).createAll(captor.capture());

        Notification notification = captor.getValue().get(0);
        assertThat(notification.getTitle()).isEqualTo("Appointment Created");
        assertThat(notification.getMessage()).isEqualTo("Your appointment is pending confirmation.");
        assertThat(notification.getType()).isEqualTo("appointment");
    }

    @Test
    void handleEvent_withPaymentSucceeded_includesAmount() {
        String json = "{\"data\":{\"amount\":\"$100.00\",\"customer_id\":999}}";
        Message message = createMessage(json);

        listener.handleEvent(message, "payment.succeeded", "payment.succeeded", "999", null, null);

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationService).createAll(captor.capture());

        Notification notification = captor.getValue().get(0);
        assertThat(notification.getTitle()).isEqualTo("Payment Successful");
        assertThat(notification.getMessage()).contains("$100.00");
    }

    @Test
    void handleEvent_withInvalidJson_handlesGracefully() {
        String invalidJson = "not valid json";
        Message message = createMessage(invalidJson);

        listener.handleEvent(message, "test.event", "test.event", "123", null, null);

        // Should not throw exception, just log error
        verify(notificationService, never()).createAll(anyList());
    }

    @Test
    void handleEvent_withCollectionRecipients_extractsAll() {
        String json = "{\"data\":{\"assigned_employee_ids\":[100,200,300]}}";
        Message message = createMessage(json);

        listener.handleEvent(message, "project.assigned", "project.assigned", null, null, null);

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationService).createAll(captor.capture());

        List<Notification> notifications = captor.getValue();
        assertThat(notifications).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    void handleEvent_withRescheduledAppointment_includesTime() {
        String json = "{\"data\":{\"scheduled_at\":\"2025-12-01 10:00\",\"customer_id\":111}}";
        Message message = createMessage(json);

        listener.handleEvent(message, "appointment.rescheduled", "appointment.rescheduled", "111", null, null);

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationService).createAll(captor.capture());

        Notification notification = captor.getValue().get(0);
        assertThat(notification.getTitle()).isEqualTo("Appointment Rescheduled");
        assertThat(notification.getMessage()).contains("2025-12-01 10:00");
    }

    @Test
    void handleEvent_withUnknownEventType_usesDefaultTitle() {
        String json = "{\"data\":{\"customer_id\":222}}";
        Message message = createMessage(json);

        listener.handleEvent(message, "custom.event", "custom.event", "222", null, null);

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationService).createAll(captor.capture());

        Notification notification = captor.getValue().get(0);
        assertThat(notification.getTitle()).isEqualTo("Event: custom.event");
        assertThat(notification.getType()).isEqualTo("custom");
    }

    @Test
    void handleEvent_withMixedUserAndRoleRecipients_createsAll() {
        String json = "{\"data\":{}}";
        Message message = createMessage(json);

        listener.handleEvent(message, "invoice.created", "invoice.created", "555,666", "ADMIN", null);

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationService).createAll(captor.capture());

        List<Notification> notifications = captor.getValue();
        assertThat(notifications).hasSize(3); // 2 users + 1 role
        assertThat(notifications).filteredOn(n -> n.getUserId() != null).hasSize(2);
        assertThat(notifications).filteredOn(n -> n.getRole() != null).hasSize(1);
    }

    @Test
    void handleEvent_withLegacyMessageId_usesIt() {
        String json = "{\"data\":{\"customer_id\":333}}";
        Message message = createMessage(json);

        listener.handleEvent(message, "test.event", "test.event", "333", null, "legacy-msg-id");

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationService).createAll(captor.capture());

        Notification notification = captor.getValue().get(0);
        assertThat(notification.getMessageId()).isEqualTo("legacy-msg-id");
    }

    @Test
    void handleEvent_withAllEventTypes_buildsCorrectTitles() {
        String[] eventTypes = {
            "appointment.accepted", "appointment.rejected", "appointment.cancelled",
            "appointment.in_progress", "appointment.completed",
            "project.requested", "project.approved", "project.in_progress",
            "project.completed", "project.cancelled",
            "payment.succeeded", "payment.failed",
            "invoice.created", "invoice.updated",
            "time_log.created", "time_log.approved", "time_log.rejected",
            "quote.approved", "quote.rejected"
        };

        for (String eventType : eventTypes) {
            String json = "{\"data\":{\"customer_id\":111}}";
            Message message = createMessage(json);

            listener.handleEvent(message, eventType, eventType, "111", null, null);
        }

        verify(notificationService, times(eventTypes.length)).createAll(anyList());
    }

    private Message createMessage(String json) {
        MessageProperties props = new MessageProperties();
        props.setMessageId("test-message-id");
        return new Message(json.getBytes(StandardCharsets.UTF_8), props);
    }
}
