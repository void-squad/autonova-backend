package com.autonova.employee_dashboard_service.dto.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskListResponse {
    private int page;
    private int pageSize;
    private int total;
    private List<TaskDto> items;
}
