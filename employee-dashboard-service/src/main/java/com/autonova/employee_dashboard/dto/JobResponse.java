package com.autonova.employee_dashboard.dto;

import com.autonova.employee_dashboard.domain.enums.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobResponse {
    private UUID id;
    private String title;
    private String description;
    private UUID projectId;
    private UUID employeeId;
    private JobStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime pausedAt;
    private LocalDateTime completedAt;
    private Integer estimatedHours;
    private Double actualHours;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
