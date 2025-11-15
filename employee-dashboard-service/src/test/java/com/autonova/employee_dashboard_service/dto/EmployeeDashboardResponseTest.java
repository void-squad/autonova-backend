package com.autonova.employee_dashboard_service.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("EmployeeDashboardResponse DTO Tests")
class EmployeeDashboardResponseTest {

    @Test
    @DisplayName("Should build complete EmployeeDashboardResponse using builder")
    void shouldBuildCompleteResponse() {
        // Given/When
        EmployeeDashboardResponse response = EmployeeDashboardResponse.builder()
                .employeeInfo(EmployeeDashboardResponse.EmployeeInfo.builder()
                        .userId(123L)
                        .name("Test Employee")
                        .email("test@autonova.com")
                        .role("EMPLOYEE")
                        .department("Service")
                        .build())
                .stats(EmployeeDashboardResponse.DashboardStats.builder()
                        .totalActiveProjects(5)
                        .pendingAppointments(3)
                        .completedTasksThisWeek(10)
                        .totalRevenueThisMonth(50000.0)
                        .totalCustomers(25)
                        .build())
                .recentActivities(new ArrayList<>())
                .upcomingTasks(new ArrayList<>())
                .activeProjects(new ArrayList<>())
                .build();

        // Then
        assertNotNull(response);
        assertNotNull(response.getEmployeeInfo());
        assertNotNull(response.getStats());
        assertNotNull(response.getRecentActivities());
        assertNotNull(response.getUpcomingTasks());
        assertNotNull(response.getActiveProjects());
    }

    @Test
    @DisplayName("Should correctly set EmployeeInfo fields")
    void shouldSetEmployeeInfoFields() {
        // Given/When
        EmployeeDashboardResponse.EmployeeInfo info = EmployeeDashboardResponse.EmployeeInfo.builder()
                .userId(456L)
                .name("John Doe")
                .email("john.doe@autonova.com")
                .role("MANAGER")
                .department("Operations")
                .build();

        // Then
        assertThat(info.getUserId()).isEqualTo(456L);
        assertThat(info.getName()).isEqualTo("John Doe");
        assertThat(info.getEmail()).isEqualTo("john.doe@autonova.com");
        assertThat(info.getRole()).isEqualTo("MANAGER");
        assertThat(info.getDepartment()).isEqualTo("Operations");
    }

    @Test
    @DisplayName("Should correctly set DashboardStats fields")
    void shouldSetDashboardStatsFields() {
        // Given/When
        EmployeeDashboardResponse.DashboardStats stats = EmployeeDashboardResponse.DashboardStats.builder()
                .totalActiveProjects(10)
                .pendingAppointments(5)
                .completedTasksThisWeek(15)
                .totalRevenueThisMonth(75000.0)
                .totalCustomers(50)
                .build();

        // Then
        assertThat(stats.getTotalActiveProjects()).isEqualTo(10);
        assertThat(stats.getPendingAppointments()).isEqualTo(5);
        assertThat(stats.getCompletedTasksThisWeek()).isEqualTo(15);
        assertThat(stats.getTotalRevenueThisMonth()).isEqualTo(75000.0);
        assertThat(stats.getTotalCustomers()).isEqualTo(50);
    }

    @Test
    @DisplayName("Should correctly set RecentActivity fields")
    void shouldSetRecentActivityFields() {
        // Given/When
        EmployeeDashboardResponse.RecentActivity activity = EmployeeDashboardResponse.RecentActivity.builder()
                .id("ACT-001")
                .type("PROJECT_UPDATE")
                .description("Updated project progress")
                .timestamp("2025-11-07 10:30:00")
                .status("COMPLETED")
                .build();

        // Then
        assertThat(activity.getId()).isEqualTo("ACT-001");
        assertThat(activity.getType()).isEqualTo("PROJECT_UPDATE");
        assertThat(activity.getDescription()).isEqualTo("Updated project progress");
        assertThat(activity.getTimestamp()).isEqualTo("2025-11-07 10:30:00");
        assertThat(activity.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("Should correctly set UpcomingTask fields")
    void shouldSetUpcomingTaskFields() {
        // Given/When
        EmployeeDashboardResponse.UpcomingTask task = EmployeeDashboardResponse.UpcomingTask.builder()
                .id("TASK-001")
                .title("Vehicle Inspection")
                .description("Complete inspection for PRJ-001")
                .dueDate("2025-11-10")
                .priority("HIGH")
                .projectId("PRJ-001")
                .build();

        // Then
        assertThat(task.getId()).isEqualTo("TASK-001");
        assertThat(task.getTitle()).isEqualTo("Vehicle Inspection");
        assertThat(task.getDescription()).isEqualTo("Complete inspection for PRJ-001");
        assertThat(task.getDueDate()).isEqualTo("2025-11-10");
        assertThat(task.getPriority()).isEqualTo("HIGH");
        assertThat(task.getProjectId()).isEqualTo("PRJ-001");
    }

    @Test
    @DisplayName("Should correctly set ProjectSummary fields")
    void shouldSetProjectSummaryFields() {
        // Given/When
        EmployeeDashboardResponse.ProjectSummary project = EmployeeDashboardResponse.ProjectSummary.builder()
                .projectId("PRJ-2024-001")
                .projectName("Vehicle Restoration")
                .customerName("John Doe")
                .status("IN_PROGRESS")
                .startDate("2025-10-01")
                .expectedCompletionDate("2025-12-01")
                .progressPercentage(65)
                .build();

        // Then
        assertThat(project.getProjectId()).isEqualTo("PRJ-2024-001");
        assertThat(project.getProjectName()).isEqualTo("Vehicle Restoration");
        assertThat(project.getCustomerName()).isEqualTo("John Doe");
        assertThat(project.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(project.getStartDate()).isEqualTo("2025-10-01");
        assertThat(project.getExpectedCompletionDate()).isEqualTo("2025-12-01");
        assertThat(project.getProgressPercentage()).isEqualTo(65);
    }

    @Test
    @DisplayName("Should support adding multiple activities to response")
    void shouldSupportMultipleActivities() {
        // Given
        List<EmployeeDashboardResponse.RecentActivity> activities = new ArrayList<>();
        activities.add(EmployeeDashboardResponse.RecentActivity.builder()
                .id("ACT-001")
                .type("PROJECT_UPDATE")
                .description("Activity 1")
                .timestamp("2025-11-07 10:00:00")
                .status("COMPLETED")
                .build());
        activities.add(EmployeeDashboardResponse.RecentActivity.builder()
                .id("ACT-002")
                .type("APPOINTMENT")
                .description("Activity 2")
                .timestamp("2025-11-07 11:00:00")
                .status("COMPLETED")
                .build());

        // When
        EmployeeDashboardResponse response = EmployeeDashboardResponse.builder()
                .employeeInfo(EmployeeDashboardResponse.EmployeeInfo.builder().build())
                .stats(EmployeeDashboardResponse.DashboardStats.builder().build())
                .recentActivities(activities)
                .upcomingTasks(new ArrayList<>())
                .activeProjects(new ArrayList<>())
                .build();

        // Then
        assertThat(response.getRecentActivities()).hasSize(2);
        assertThat(response.getRecentActivities().get(0).getId()).isEqualTo("ACT-001");
        assertThat(response.getRecentActivities().get(1).getId()).isEqualTo("ACT-002");
    }

    @Test
    @DisplayName("Should use no-args constructor and setters")
    void shouldUseNoArgsConstructorAndSetters() {
        // Given/When
        EmployeeDashboardResponse response = new EmployeeDashboardResponse();
        response.setEmployeeInfo(EmployeeDashboardResponse.EmployeeInfo.builder()
                .userId(789L)
                .name("Test")
                .build());

        // Then
        assertNotNull(response.getEmployeeInfo());
        assertThat(response.getEmployeeInfo().getUserId()).isEqualTo(789L);
    }
}
