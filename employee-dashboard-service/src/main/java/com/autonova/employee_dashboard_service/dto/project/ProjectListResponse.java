package com.autonova.employee_dashboard_service.dto.project;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectListResponse {
    private int page;
    private int pageSize;
    private int total;
    private List<ProjectDto> items;
}
