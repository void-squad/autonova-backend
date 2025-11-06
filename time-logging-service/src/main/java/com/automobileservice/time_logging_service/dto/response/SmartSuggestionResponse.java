package com.automobileservice.time_logging_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmartSuggestionResponse {
    
    private TaskResponse task;
    private String projectTitle;
    private String reason;
    private String urgency; // "high", "medium", "low"
    private String icon; // "deadline", "progress", "efficiency", "priority"
}
