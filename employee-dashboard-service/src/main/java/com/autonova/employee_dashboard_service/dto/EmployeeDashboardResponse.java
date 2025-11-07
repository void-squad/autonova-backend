package com.autonova.employee_dashboard_service.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDashboardResponse {
    private EmployeeInfo employeeInfo;
    private DashboardStats stats;
    private List<RecentActivity> recentActivities;
    private List<UpcomingTask> upcomingTasks;
    private List<ProjectSummary> activeProjects;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeeInfo {
        private Long userId;
        private String name;
        private String email;
        private String role;
        private String department;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardStats {
        private int totalActiveProjects;
        private int pendingAppointments;
        private int completedTasksThisWeek;
        private double totalRevenueThisMonth;
        private int totalCustomers;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivity {
        private String id;
        private String type;
        private String description;
        private String timestamp;
        private String status;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpcomingTask {
        private String id;
        private String title;
        private String description;
        private String dueDate;
        private String priority;
        private String projectId;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectSummary {
        private String projectId;
        private String projectName;
        private String customerName;
        private String status;
        private String startDate;
        private String expectedCompletionDate;
        private int progressPercentage;
    }
}
