package com.automobileservice.time_logging_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskResponse {
    
    private String id;
    private String projectId;
    private String taskName;
    private String description;
    private String assignedEmployeeId;
    private String assignedEmployeeName;
    private BigDecimal estimatedHours;
    private BigDecimal actualHours;
    private String status;
    private String priority;
    private LocalDate dueDate;
}