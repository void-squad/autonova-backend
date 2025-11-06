package com.automobileservice.time_logging_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeLogResponse {
    
    private String id;
    private String projectId;
    private String projectTitle;
    private String taskId;
    private String taskName;
    private String employeeId;
    private String employeeName;
    private BigDecimal hours;
    private String note;
    private LocalDateTime loggedAt;
}