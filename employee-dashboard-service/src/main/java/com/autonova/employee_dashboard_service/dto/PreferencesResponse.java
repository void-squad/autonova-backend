package com.autonova.employee_dashboard_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreferencesResponse {
    private Long employeeId;
    private String defaultView; // "operational" or "analytical"
    private String theme; // "dark" or "light"
}
