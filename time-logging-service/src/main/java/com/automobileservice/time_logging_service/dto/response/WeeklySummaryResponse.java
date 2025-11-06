package com.automobileservice.time_logging_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklySummaryResponse {
    
    private List<DailyHours> dailyHours;
    private List<ProjectBreakdown> projectBreakdown;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyHours {
        private String day; // e.g., "Mon", "Tue", etc.
        private BigDecimal hours;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProjectBreakdown {
        private String projectId;
        private String projectTitle;
        private Integer taskCount;
        private BigDecimal totalHours;
    }
}
