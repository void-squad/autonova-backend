package com.automobileservice.time_logging_service.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeLogRequest {
    
    @NotNull(message = "Employee ID is required")
    private Long employeeId;
    
    // Frontend gets this from project dropdown selection
    @NotNull(message = "Project ID is required")
    private UUID projectId;
    
    // Frontend gets this from task dropdown selection
    @NotNull(message = "Task ID is required")
    private UUID taskId;
    
    @Positive(message = "Hours must be greater than 0")
    private BigDecimal hours;
    
    // Optional notes
    private String note;
}