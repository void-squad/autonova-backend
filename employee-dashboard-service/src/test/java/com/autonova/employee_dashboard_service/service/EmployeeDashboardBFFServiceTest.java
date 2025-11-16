package com.autonova.employee_dashboard_service.service;

import com.autonova.employee_dashboard_service.dto.EmployeeDashboardResponse;
import com.autonova.employee_dashboard_service.dto.task.TaskDto;
import com.autonova.employee_dashboard_service.dto.task.TaskListResponse;
import com.autonova.employee_dashboard_service.dto.timelog.TimeLogDto;
import com.autonova.employee_dashboard_service.dto.timelog.TimeLogListResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@DisplayName("EmployeeDashboardBFFService Unit Tests")
@ExtendWith(MockitoExtension.class)
class EmployeeDashboardBFFServiceTest {

    private EmployeeDashboardBFFService service;

    @Mock
    private ProjectServiceClient projectServiceClient;

    @Mock
    private TimeLoggingServiceClient timeLoggingServiceClient;

    private Long testUserId;
    private String testUserEmail;
    private String testUserRole;
    private String testToken;

    @BeforeEach
    void setUp() {
        testUserId = 123L;
        testUserEmail = "test.employee@autonova.com";
        testUserRole = "EMPLOYEE";
        testToken = "Bearer test-token";

        service = new EmployeeDashboardBFFService(projectServiceClient, timeLoggingServiceClient);

        TaskDto task = TaskDto.builder()
            .taskId("task-1")
            .title("Vehicle inspection")
            .description("Complete vehicle inspection before delivery")
            .status("Scheduled")
            .scheduledStart(OffsetDateTime.now().plusDays(1))
            .scheduledEnd(OffsetDateTime.now().plusDays(2))
            .build();

        TaskListResponse taskList = TaskListResponse.builder()
            .page(1)
            .pageSize(50)
            .total(1)
            .items(List.of(task))
            .build();

        lenient().when(projectServiceClient.getTasksByAssignee(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.isNull(),
            anyInt(),
            anyInt(),
            ArgumentMatchers.anyString()
        )).thenReturn(Mono.just(taskList));

        TimeLogDto timeLog = TimeLogDto.builder()
            .id("log-1")
            .projectId("project-1")
            .taskId("task-1")
            .hours(BigDecimal.valueOf(3.5))
            .approvalStatus("PENDING")
            .note("Initial inspection")
            .loggedAt(LocalDateTime.now())
            .build();

        TimeLogListResponse timeLogList = TimeLogListResponse.builder()
            .page(1)
            .pageSize(50)
            .total(1)
            .items(List.of(timeLog))
            .build();

        lenient().when(timeLoggingServiceClient.getTimeLogsByEmployee(
            ArgumentMatchers.anyString(),
            anyInt(),
            anyInt(),
            ArgumentMatchers.anyString()
        )).thenReturn(Mono.just(timeLogList));
    }

    @Test
    @DisplayName("Should return complete dashboard response with all sections")
    void shouldReturnCompleteDashboardResponse() {
        // When
        EmployeeDashboardResponse response = service.getEmployeeDashboard(testUserId, testUserEmail, testUserRole, testToken).block();

        // Then
        assertNotNull(response, "Dashboard response should not be null");
        assertNotNull(response.getEmployeeInfo(), "Employee info should be present");
        assertNotNull(response.getStats(), "Dashboard stats should be present");
        assertNotNull(response.getRecentActivities(), "Recent activities should be present");
        assertNotNull(response.getUpcomingTasks(), "Upcoming tasks should be present");
        assertNotNull(response.getRecentTimeLogs(), "Recent time logs should be present");
        assertNotNull(response.getActiveProjects(), "Active projects should be present");
    }

    @Test
    @DisplayName("Should return correct employee info")
    void shouldReturnCorrectEmployeeInfo() {
        // When
        EmployeeDashboardResponse response = service.getEmployeeDashboard(testUserId, testUserEmail, testUserRole, testToken).block();

        // Then
        EmployeeDashboardResponse.EmployeeInfo info = response.getEmployeeInfo();
        assertThat(info.getUserId()).isEqualTo(testUserId);
        assertThat(info.getEmail()).isEqualTo(testUserEmail);
        assertThat(info.getRole()).isEqualTo(testUserRole);
        assertThat(info.getName()).isNotBlank();
        assertThat(info.getDepartment()).isNotBlank();
    }

    @Test
    @DisplayName("Should return valid dashboard stats")
    void shouldReturnValidDashboardStats() {
        // When
        EmployeeDashboardResponse response = service.getEmployeeDashboard(testUserId, testUserEmail, testUserRole, testToken).block();

        // Then
        EmployeeDashboardResponse.DashboardStats stats = response.getStats();
        assertThat(stats.getTotalActiveProjects()).isGreaterThanOrEqualTo(0);
        assertThat(stats.getPendingAppointments()).isGreaterThanOrEqualTo(0);
        assertThat(stats.getCompletedTasksThisWeek()).isGreaterThanOrEqualTo(0);
        assertThat(stats.getTotalRevenueThisMonth()).isGreaterThanOrEqualTo(0.0);
        assertThat(stats.getTotalCustomers()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Should return recent activities list")
    void shouldReturnRecentActivities() {
        // When
        EmployeeDashboardResponse response = service.getEmployeeDashboard(testUserId, testUserEmail, testUserRole, testToken).block();

        // Then
        assertThat(response.getRecentActivities()).isEmpty();
    }

    @Test
    @DisplayName("Should return upcoming tasks list")
    void shouldReturnUpcomingTasks() {
        // When
        EmployeeDashboardResponse response = service.getEmployeeDashboard(testUserId, testUserEmail, testUserRole, testToken).block();

        // Then
        assertThat(response.getUpcomingTasks()).isNotEmpty();
        assertThat(response.getUpcomingTasks().get(0).getTitle()).isEqualTo("Vehicle inspection");
    }

    @Test
    @DisplayName("Should return recent time logs list")
    void shouldReturnRecentTimeLogs() {
        EmployeeDashboardResponse response = service.getEmployeeDashboard(testUserId, testUserEmail, testUserRole, testToken).block();

        assertThat(response.getRecentTimeLogs()).isNotEmpty();
        assertThat(response.getRecentTimeLogs().get(0).getId()).isEqualTo("log-1");
    }

    @Test
    @DisplayName("Should return empty tasks list when project service fails")
    void shouldReturnEmptyTasksWhenProjectServiceFails() {
        // Given
        when(projectServiceClient.getTasksByAssignee(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.isNull(),
                anyInt(),
                anyInt(),
                ArgumentMatchers.anyString()
        )).thenReturn(Mono.error(new RuntimeException("project service down")));

        // When
        EmployeeDashboardResponse response = service.getEmployeeDashboard(testUserId, testUserEmail, testUserRole, testToken).block();

        // Then
        assertNotNull(response);
        assertThat(response.getUpcomingTasks()).isEmpty();
    }

    @Test
    @DisplayName("Should return empty time logs when time logging service fails")
    void shouldReturnEmptyTimeLogsWhenServiceFails() {
        when(timeLoggingServiceClient.getTimeLogsByEmployee(
                ArgumentMatchers.anyString(),
                anyInt(),
                anyInt(),
                ArgumentMatchers.anyString()
        )).thenReturn(Mono.error(new RuntimeException("time logging down")));

        EmployeeDashboardResponse response = service.getEmployeeDashboard(testUserId, testUserEmail, testUserRole, testToken).block();

        assertNotNull(response);
        assertThat(response.getRecentTimeLogs()).isEmpty();
    }

    @Test
    @DisplayName("Should return active projects list")
    void shouldReturnActiveProjects() {
        // When
        EmployeeDashboardResponse response = service.getEmployeeDashboard(testUserId, testUserEmail, testUserRole, testToken).block();

        // Then
        assertThat(response.getActiveProjects()).isEmpty();
    }

    @Test
    @DisplayName("Should handle different user roles")
    void shouldHandleDifferentUserRoles() {
        // Given
        String managerRole = "MANAGER";

        // When
        EmployeeDashboardResponse response = service.getEmployeeDashboard(testUserId, testUserEmail, managerRole, testToken).block();

        // Then
        assertNotNull(response);
        assertThat(response.getEmployeeInfo().getRole()).isEqualTo(managerRole);
    }

    @Test
    @DisplayName("Should handle different user IDs")
    void shouldHandleDifferentUserIds() {
        // Given
        Long differentUserId = 999L;

        // When
        EmployeeDashboardResponse response = service.getEmployeeDashboard(differentUserId, testUserEmail, testUserRole, testToken).block();

        // Then
        assertNotNull(response);
        assertThat(response.getEmployeeInfo().getUserId()).isEqualTo(differentUserId);
    }

    @Test
    @DisplayName("Should throw when userId is null")
    void shouldThrowWhenUserIdIsNull() {
        assertThrows(NullPointerException.class, () ->
                service.getEmployeeDashboard(null, testUserEmail, testUserRole, testToken).block());
    }

    @Test
    @DisplayName("Should return consistent data structure across multiple calls")
    void shouldReturnConsistentDataStructure() {
        // When
        EmployeeDashboardResponse response1 = service.getEmployeeDashboard(testUserId, testUserEmail, testUserRole, testToken).block();
        EmployeeDashboardResponse response2 = service.getEmployeeDashboard(testUserId, testUserEmail, testUserRole, testToken).block();

        // Then
        assertThat(response1.getRecentActivities()).hasSameSizeAs(response2.getRecentActivities());
        assertThat(response1.getUpcomingTasks()).hasSameSizeAs(response2.getUpcomingTasks());
        assertThat(response1.getRecentTimeLogs()).hasSameSizeAs(response2.getRecentTimeLogs());
        assertThat(response1.getActiveProjects()).hasSameSizeAs(response2.getActiveProjects());
    }
}
