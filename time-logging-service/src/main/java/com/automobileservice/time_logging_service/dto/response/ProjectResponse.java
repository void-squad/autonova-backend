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
public class ProjectResponse {
    
    private String id;
    private String customerId;
    private String customerName;
    private String vehicleId;
    private String vehicleInfo; // "2020 Toyota Camry"
    private String projectType;
    private String title;
    private String description;
    private String status;
    private String priority;
    private BigDecimal estimatedCost;
    private LocalDate startDate;
    private LocalDate endDate;
}