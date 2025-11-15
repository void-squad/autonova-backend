package com.autonova.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardAnalyticsDto {
    private StatsDto stats;
    private List<ActivityDto> recentActivity;
    private List<EmployeePerformanceDto> topEmployees;
}