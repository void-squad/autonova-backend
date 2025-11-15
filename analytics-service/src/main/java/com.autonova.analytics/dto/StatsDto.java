package com.autonova.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatsDto {
    private long totalCustomers;
    private long activeAppointments;
    private double monthlyRevenue;
    private long activeProjects;
}