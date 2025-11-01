package com.autonova.employee_dashboard_service.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveReportRequest {
    private String reportName;
    private Map<String, Object> reportParameters;
}
