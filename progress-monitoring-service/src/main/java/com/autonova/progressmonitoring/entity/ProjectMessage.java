package com.autonova.progressmonitoring.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
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
    @GeneratedValue(strategy = GenerationType.UUID)
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
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private OffsetDateTime createdAt;

    // Attachment fields (optional)
    @Column(name = "attachment_url")
    private String attachmentUrl;

    @Column(name = "attachment_content_type")
    private String attachmentContentType;

    @Column(name = "attachment_filename")
    private String attachmentFilename;

    @Column(name = "attachment_size")
    private Long attachmentSize;

    public ProjectMessage(UUID projectId, String category, String message, String payload, OffsetDateTime occurredAt) {
        this.projectId = projectId;
        this.category = category;
        this.message = message;
        this.payload = payload;
        this.occurredAt = occurredAt;
    }
}
