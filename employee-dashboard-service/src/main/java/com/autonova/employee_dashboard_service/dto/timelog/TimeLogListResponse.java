package com.autonova.employee_dashboard_service.dto.timelog;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeLogListResponse {
    private int page;
    private int pageSize;
    private int total;
    private List<TimeLogDto> items;
}
