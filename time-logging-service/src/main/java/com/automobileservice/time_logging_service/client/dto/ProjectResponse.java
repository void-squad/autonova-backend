package com.automobileservice.time_logging_service.client.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class ProjectResponse {
    private UUID projectId;
    private String title;
    private String status;
    private String description;
}
