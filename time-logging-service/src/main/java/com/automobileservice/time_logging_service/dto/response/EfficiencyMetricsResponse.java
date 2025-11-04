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
public class EfficiencyMetricsResponse {
    
    private BigDecimal efficiency; // Percentage (0-100)
    private BigDecimal weeklyTrend; // Percentage change from last week
    private EfficiencyBreakdown breakdown;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EfficiencyBreakdown {
        private BigDecimal onTime; // Percentage of tasks completed on time
        private BigDecimal overEstimate; // Percentage of tasks that went over estimate
        private BigDecimal avgTaskTime; // Average hours per task
    }
}
