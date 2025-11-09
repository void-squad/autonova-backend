package com.autonova.employee_dashboard_service.dto.task;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    private String projectId;
    private String title;
    private String description;
    private String assigneeId;
    private String status;
    private BigDecimal estimateHours;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime createdAt;
    
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
