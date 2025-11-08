// For aggregated hourly summaries per employee
package com.automobileservice.time_logging_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeSummaryResponse {
    
    private String employeeId;
    private String employeeName;
    private String department;
    private BigDecimal totalHoursLogged;
    private BigDecimal hourlyRate;
    private BigDecimal totalEarnings; // totalHours * hourlyRate
}