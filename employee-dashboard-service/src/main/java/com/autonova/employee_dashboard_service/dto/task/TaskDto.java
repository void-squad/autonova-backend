package com.autonova.employee_dashboard_service.dto.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDto {
    private String taskId;
    private String title;
    private String serviceType;
    @JsonProperty("detail")
    private String description;
    private String assigneeId;
    private String status;
    private BigDecimal estimateHours;
    private String appointmentId;

    private OffsetDateTime scheduledStart;

    private OffsetDateTime scheduledEnd;
    
    private OffsetDateTime createdAt;
    
    private OffsetDateTime updatedAt;

    private String projectId;
    private TaskProjectSummary project;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskProjectSummary {
        private String projectId;
        private String title;
        private String status;
    }
}
