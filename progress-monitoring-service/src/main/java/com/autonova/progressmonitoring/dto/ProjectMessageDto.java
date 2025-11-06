package com.autonova.progressmonitoring.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class ProjectMessageDto {
    private UUID id;
    private UUID projectId;
    private String category;
    private String message;
    private String payload;
    private OffsetDateTime occurredAt;
    private OffsetDateTime createdAt;
}
