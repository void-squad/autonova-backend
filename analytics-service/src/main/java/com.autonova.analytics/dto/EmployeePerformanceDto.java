package com.autonova.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeePerformanceDto {
    private String name;
    private int tasksCompleted;
    private int hoursLogged;
    private int efficiency;
}