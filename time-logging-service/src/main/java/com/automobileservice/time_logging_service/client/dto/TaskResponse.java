package com.automobileservice.time_logging_service.client.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class TaskResponse {
    private UUID taskId;
    private UUID projectId;
    private String title;
    private String serviceType;
    private String detail;
    private String status;
    private UUID assigneeId;
    private Double estimateHours;
    private LocalDateTime scheduledStart;
    private LocalDateTime scheduledEnd;
}
