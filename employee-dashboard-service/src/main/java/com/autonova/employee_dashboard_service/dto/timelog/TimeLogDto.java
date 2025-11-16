package com.autonova.employee_dashboard_service.dto.timelog;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeLogDto {
    private String id;
    private String projectId;
    private String projectTitle;
    private String taskId;
    private String taskName;
    private Long employeeId;
    private String employeeName;
    private BigDecimal hours;
    private String note;
    private String approvalStatus;
    private String rejectionReason;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private LocalDateTime loggedAt;
}
