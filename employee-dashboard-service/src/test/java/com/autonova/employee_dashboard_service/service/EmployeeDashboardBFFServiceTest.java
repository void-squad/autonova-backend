package com.autonova.employee_dashboard_service.service;

import com.autonova.employee_dashboard_service.dto.EmployeeDashboardResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeDashboardBFFService Unit Tests")
class EmployeeDashboardBFFServiceTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @InjectMocks
    private EmployeeDashboardBFFService service;

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
        assertThat(response.getRecentActivities()).isNotEmpty();
        response.getRecentActivities().forEach(activity -> {
            assertThat(activity.getId()).isNotBlank();
            assertThat(activity.getType()).isNotBlank();
            assertThat(activity.getDescription()).isNotBlank();
            assertThat(activity.getTimestamp()).isNotBlank();
            assertThat(activity.getStatus()).isNotBlank();
        });
    }

    @Test
    @DisplayName("Should return upcoming tasks list")
    void shouldReturnUpcomingTasks() {
        // When
        EmployeeDashboardResponse response = service.getEmployeeDashboard(testUserId, testUserEmail, testUserRole, testToken).block();

        // Then
        assertThat(response.getUpcomingTasks()).isNotEmpty();
        response.getUpcomingTasks().forEach(task -> {
            assertThat(task.getId()).isNotBlank();
            assertThat(task.getTitle()).isNotBlank();
            assertThat(task.getDueDate()).isNotBlank();
            assertThat(task.getPriority()).isNotBlank();
        });
    }

    @Test
    @DisplayName("Should return active projects list")
    void shouldReturnActiveProjects() {
        // When
        EmployeeDashboardResponse response = service.getEmployeeDashboard(testUserId, testUserEmail, testUserRole, testToken).block();

        // Then
        assertThat(response.getActiveProjects()).isNotEmpty();
        response.getActiveProjects().forEach(project -> {
            assertThat(project.getProjectId()).isNotBlank();
            assertThat(project.getProjectName()).isNotBlank();
            assertThat(project.getStatus()).isNotBlank();
        });
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
    @DisplayName("Should handle null userId gracefully")
    void shouldHandleNullUserId() {
        // When/Then - should not throw exception
        assertDoesNotThrow(() -> 
            service.getEmployeeDashboard(null, testUserEmail, testUserRole, testToken).block()
        );
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
        assertThat(response1.getActiveProjects()).hasSameSizeAs(response2.getActiveProjects());
    }
}
