package com.automobileservice.time_logging_service.client.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class TaskResponse {
    private String id;
    private String title;
    private String projectId;
    private String assignedEmployeeId;
    private LocalDate dueDate;
    private String status;
}
