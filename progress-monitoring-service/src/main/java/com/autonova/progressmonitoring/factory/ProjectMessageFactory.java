package com.autonova.progressmonitoring.factory;

import com.autonova.progressmonitoring.dto.ProjectMessageDto;
import com.autonova.progressmonitoring.storage.StoredAttachment;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class ProjectMessageFactory {

    private ProjectMessageFactory() { }

    public static ProjectMessageDto fromManual(UUID projectId,
                                               String category,
                                               String message,
                                               String payload,
                                               OffsetDateTime occurredAt) {
        return ProjectMessageDto.builder()
                .projectId(projectId)
                .category(category)
                .message(message)
                .payload(payload)
                .occurredAt(occurredAt)
                .build();
    }

    // New overload: manual message with attachment metadata
    public static ProjectMessageDto fromManual(UUID projectId,
                                               String category,
                                               String message,
                                               String payload,
                                               OffsetDateTime occurredAt,
                                               String attachmentUrl,
                                               String attachmentContentType,
                                               String attachmentFilename,
                                               Long attachmentSize) {
        return ProjectMessageDto.builder()
                .projectId(projectId)
                .category(category)
                .message(message)
                .payload(payload)
                .occurredAt(occurredAt)
                .attachmentUrl(attachmentUrl)
                .attachmentContentType(attachmentContentType)
                .attachmentFilename(attachmentFilename)
                .attachmentSize(attachmentSize)
                .build();
    }

    public static ProjectMessageDto fromManualWithAttachment(UUID projectId,
                                                             String category,
                                                             String message,
                                                             StoredAttachment attachment) {
        if (attachment == null) throw new IllegalArgumentException("attachment cannot be null");
        return ProjectMessageDto.builder()
                .projectId(projectId)
                .category(category)
                .message(message)
                .occurredAt(OffsetDateTime.now())
                .attachmentUrl(attachment.getUrl())
                .attachmentContentType(attachment.getContentType())
                .attachmentFilename(attachment.getOriginalFilename())
                .attachmentSize(attachment.getSize())
                .build();
    }

    public static ProjectMessageDto fromEvent(UUID projectId,
                                              String category,
                                              String friendlyMessage,
                                              String rawPayload,
                                              OffsetDateTime occurredAt) {
        return ProjectMessageDto.builder()
                .projectId(projectId)
                .category(category)
                .message(friendlyMessage)
                .payload(rawPayload)
                .occurredAt(occurredAt)
                .build();
    }
}
