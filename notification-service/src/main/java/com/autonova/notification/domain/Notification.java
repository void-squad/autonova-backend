package com.autonova.notification.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notifications_user_read", columnList = "userId,readFlag"),
        @Index(name = "idx_notifications_event_type", columnList = "eventType")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_notifications_msg_user", columnNames = {"messageId", "userId"})
})
public class Notification {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    // For idempotency across recipients
    @Column(length = 100, updatable = false)
    private String messageId;

    // Target user; nullable when targeting role broadcast only
    private Long userId; // changed from UUID to Long (bigint)

    // Optional role recipient (CUSTOMER|EMPLOYEE|ADMIN) if broadcast by role
    @Column(length = 32)
    private String role;

    @Column(nullable = false)
    private String type; // logical grouping (e.g., project, appointment, payment)

    @Column(nullable = false)
    private String eventType; // raw event routing key: appointment.created etc.

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 2000)
    private String message;

    @Column(columnDefinition = "TEXT")
    private String eventPayload; // raw JSON for client detail if needed

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private boolean readFlag = false;

    public Notification() {}

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }

    // Getters / Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getEventPayload() { return eventPayload; }
    public void setEventPayload(String eventPayload) { this.eventPayload = eventPayload; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public boolean isReadFlag() { return readFlag; }
    public void setReadFlag(boolean readFlag) { this.readFlag = readFlag; }
}
