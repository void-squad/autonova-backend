package com.automobileservice.time_logging_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeLogRequest {
    
    @NotBlank(message = "Employee ID is required")
    private String employeeId;
    
    // Frontend gets this from project dropdown selection
    @NotBlank(message = "Project ID is required")
    private String projectId;
    
    // Frontend gets this from task dropdown selection
    @NotBlank(message = "Task ID is required")
    private String taskId;
    
    @Positive(message = "Hours must be greater than 0")
    private BigDecimal hours;
    
    // Optional notes
    private String note;
}