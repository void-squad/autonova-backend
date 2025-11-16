package com.automobileservice.time_logging_service.client.dto;

import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class TaskResponse {
    private UUID taskId;
    private UUID projectId;
    private String title;
    private String serviceType;
    private String detail;
    private String status;
    private Long assigneeId;
    private Double estimateHours;
    private OffsetDateTime scheduledStart;
    private OffsetDateTime scheduledEnd;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
