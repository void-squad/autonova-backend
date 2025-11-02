// ...existing code...
package com.autonova.progressmonitoring.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "project_messages")
public class ProjectMessage {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "category")
    private String category;

    @Column(name = "message", columnDefinition = "text")
    private String message;

    @Column(name = "payload", columnDefinition = "text")
    private String payload;

    @Column(name = "occurred_at")
    private OffsetDateTime occurredAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected ProjectMessage() {
        // for JPA
    }

    public ProjectMessage(UUID projectId, String category, String message, String payload, OffsetDateTime occurredAt) {
        this.projectId = projectId;
        this.category = category;
        this.message = message;
        this.payload = payload;
        this.occurredAt = occurredAt;
        this.createdAt = OffsetDateTime.now();
    }

    // getters and setters
    public UUID getId() { return id; }
    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public OffsetDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(OffsetDateTime occurredAt) { this.occurredAt = occurredAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}

