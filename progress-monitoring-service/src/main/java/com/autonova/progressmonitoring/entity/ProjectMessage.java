// ...existing code...
package com.autonova.progressmonitoring.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "project_messages")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ProjectMessage {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    @Setter(AccessLevel.NONE)
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
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public ProjectMessage(UUID projectId, String category, String message, String payload, OffsetDateTime occurredAt) {
        this.projectId = projectId;
        this.category = category;
        this.message = message;
        this.payload = payload;
        this.occurredAt = occurredAt;
        this.createdAt = OffsetDateTime.now();
    }
}

