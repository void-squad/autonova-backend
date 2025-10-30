package com.autonova.employee_dashboard.dto;

import com.autonova.employee_dashboard.domain.enums.JobStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateJobStatusRequest {
    @NotNull(message = "Status is required")
    private JobStatus status;
    
    private String notes;
}
