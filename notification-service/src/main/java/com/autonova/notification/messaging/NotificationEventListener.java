package com.autonova.notification.messaging;

import com.autonova.notification.domain.Notification;
import com.autonova.notification.service.NotificationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.amqp.support.AmqpHeaders.RECEIVED_ROUTING_KEY;

@Service
public class NotificationEventListener {
    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public NotificationEventListener(NotificationService notificationService, ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "${app.rabbit.queue}")
    public void handleEvent(
            Message message,
            @Header(RECEIVED_ROUTING_KEY) String routingKey,
            @Header(name = "x-event-name", required = false) String eventName,
            @Header(name = "x-recipients-user-ids", required = false) String recipientsUserIds,
            @Header(name = "x-recipients-roles", required = false) String recipientsRoles,
            @Header(name = "message_id", required = false) String legacyMessageId
    ) {
        String json = new String(message.getBody(), StandardCharsets.UTF_8);
        String effectiveEventType = eventName != null ? eventName : routingKey;
        String msgPropId = message.getMessageProperties().getMessageId();
        String effectiveMessageId = firstNonBlank(legacyMessageId, msgPropId, java.util.UUID.randomUUID().toString());
        try {
            Map<String, Object> payload = objectMapper.readValue(json, new TypeReference<>() {});
            Object dataObj = payload.getOrDefault("data", Collections.emptyMap());
            Map<String, Object> data = dataObj instanceof Map ? (Map<String, Object>) dataObj : Collections.emptyMap();
            Set<Long> userRecipients = parseLongCsv(recipientsUserIds);
            Set<String> roleRecipients = parseCsv(recipientsRoles);
            if (userRecipients.isEmpty()) userRecipients = deriveRecipientsFromPayload(data);
            if (userRecipients.isEmpty() && roleRecipients.isEmpty()) {
                log.warn("No recipients provided/derived for event {}. Users='{}' Roles='{}'", effectiveEventType, recipientsUserIds, recipientsRoles);
                return;
            }
            String groupType = deriveGroupType(effectiveEventType);
            String title = buildTitle(effectiveEventType, data);
            String messageText = buildMessage(effectiveEventType, data);
            List<Notification> notifications = new ArrayList<>();
            for (Long userId : userRecipients) notifications.add(baseNotification(effectiveMessageId, userId, null, groupType, effectiveEventType, title, messageText, json));
            for (String role : roleRecipients) notifications.add(baseNotification(effectiveMessageId + ":" + role, null, role.toUpperCase(Locale.ROOT), groupType, effectiveEventType, title, messageText, json));
            notificationService.createAll(notifications);
        } catch (Exception ex) {
            log.error("Failed to process message routingKey={}, eventType={}, error={}", routingKey, effectiveEventType, ex.getMessage(), ex);
        }
    }

    private Set<Long> deriveRecipientsFromPayload(Map<String, Object> data) {
        Set<Long> ids = new HashSet<>();
        // common keys
        Stream.of("customer_id", "user_id", "employee_id", "assigned_employee_id")
                .map(data::get)
                .map(this::toLong)
                .filter(Objects::nonNull)
                .forEach(ids::add);
        Object list = data.get("assigned_employee_ids");
        if (list instanceof Collection<?> c) {
            for (Object v : c) {
                Long id = toLong(v);
                if (id != null) ids.add(id);
            }
        }
        return ids;
    }

    private Notification baseNotification(String messageId, Long userId, String role, String type, String eventType, String title, String msg, String rawJson) {
        Notification n = new Notification();
        n.setMessageId(messageId);
        n.setUserId(userId);
        n.setRole(role);
        n.setType(type);
        n.setEventType(eventType);
        n.setTitle(title);
        n.setMessage(msg);
        n.setEventPayload(rawJson);
        n.setCreatedAt(Instant.now());
        return n;
    }

    private String deriveGroupType(String eventType) {
        if (eventType == null) return "event";
        int dot = eventType.indexOf('.');
        return dot > 0 ? eventType.substring(0, dot) : eventType;
    }

    private String buildTitle(String eventType, Map<String, Object> data) {
        return switch (eventType) {
            case "appointment.created" -> "Appointment Created";
            case "appointment.accepted" -> "Appointment Accepted";
            case "appointment.rejected" -> "Appointment Rejected";
            case "appointment.rescheduled" -> "Appointment Rescheduled";
            case "appointment.cancelled" -> "Appointment Cancelled";
            case "appointment.in_progress" -> "Appointment In Progress";
            case "appointment.completed" -> "Appointment Completed";
            case "project.requested" -> "Modification Request Submitted";
            case "project.approved" -> "Project Approved";
            case "project.in_progress" -> "Project In Progress";
            case "project.completed" -> "Project Completed";
            case "project.cancelled" -> "Project Cancelled";
            case "payment.succeeded" -> "Payment Successful";
            case "payment.failed" -> "Payment Failed";
            case "invoice.created" -> "Invoice Created";
            case "invoice.updated" -> "Invoice Updated";
            case "time_log.created" -> "Time Log Submitted";
            case "time_log.approved" -> "Time Log Approved";
            case "time_log.rejected" -> "Time Log Rejected";
            case "quote.approved" -> "Quote Approved";
            case "quote.rejected" -> "Quote Rejected";
            default -> "Event: " + eventType;
        };
    }

    private String buildMessage(String eventType, Map<String, Object> data) {
        try {
            return switch (eventType) {
                case "appointment.created" -> "Your appointment is pending confirmation.";
                case "appointment.accepted" -> "Your appointment has been accepted.";
                case "appointment.rejected" -> "Your appointment was rejected.";
                case "appointment.rescheduled" -> "Appointment rescheduled to " + data.getOrDefault("scheduled_at", "new time") + ".";
                case "appointment.cancelled" -> "Your appointment was cancelled.";
                case "appointment.in_progress" -> "Service work has started on your vehicle.";
                case "appointment.completed" -> "Service appointment completed.";
                case "project.requested" -> "Your modification request is awaiting review.";
                case "project.approved" -> "Your project has been approved.";
                case "project.in_progress" -> "Project is now in progress.";
                case "project.completed" -> "Project completed. Please review.";
                case "project.cancelled" -> "Project was cancelled.";
                case "payment.succeeded" -> "Payment received: " + data.getOrDefault("amount", "");
                case "payment.failed" -> "Payment failed. Please retry.";
                case "invoice.created" -> "A new invoice is available.";
                case "invoice.updated" -> "Invoice updated.";
                case "time_log.created" -> "New time log submitted.";
                case "time_log.approved" -> "Your time log was approved.";
                case "time_log.rejected" -> "Your time log was rejected.";
                case "quote.approved" -> "Your quote was approved.";
                case "quote.rejected" -> "Your quote was rejected.";
                default -> "Event payload: " + safeToString(data);
            };
        } catch (Exception e) {
            return "Event received.";
        }
    }

    private Set<Long> parseLongCsv(String csv) {
        return parseCsv(csv).stream().map(this::toLong).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private Set<String> parseCsv(String csv) {
        if (csv == null || csv.isBlank()) return Collections.emptySet();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private Long toLong(Object raw) {
        try {
            if (raw == null) return null;
            String s = String.valueOf(raw).trim();
            if (s.isEmpty()) return null;
            return Long.parseLong(s);
        } catch (Exception e) {
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        for (String v : values) if (v != null && !v.isBlank()) return v;
        return null;
    }

    private String safeToString(Object o) {
        try { return String.valueOf(o); } catch (Exception e) { return ""; }
    }
}
