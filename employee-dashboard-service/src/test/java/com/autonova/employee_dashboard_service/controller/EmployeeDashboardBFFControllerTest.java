package com.autonova.employee_dashboard_service.controller;

import com.autonova.employee_dashboard_service.dto.EmployeeDashboardResponse;
import com.autonova.employee_dashboard_service.security.JwtAuthenticationFilter;
import com.autonova.employee_dashboard_service.service.EmployeeDashboardBFFService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller Integration Tests - Simple tests for endpoints
 * Note: Full security integration would require running auth-service or mocking JWT tokens
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("EmployeeDashboardBFFController Basic Tests")
class EmployeeDashboardBFFControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeDashboardBFFService bffService;

    @Test
    @DisplayName("Should return 200 OK for health check endpoint with authenticated user")
    @WithMockUser(roles = {"EMPLOYEE"})
    void shouldReturnHealthCheck() throws Exception {
        mockMvc.perform(get("/api/employee/dashboard/health").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Employee Dashboard BFF is running"));
    }

    @Test
    @DisplayName("Should return dashboard data for authenticated employee with role")
    @WithMockUser(username = "employee@autonova.com", roles = {"EMPLOYEE"})
    void shouldReturnDashboardDataForAuthenticatedEmployee() throws Exception {
        // Given
        EmployeeDashboardResponse mockResponse = EmployeeDashboardResponse.builder()
                .employeeInfo(EmployeeDashboardResponse.EmployeeInfo.builder()
                        .userId(123L)
                        .name("Test Employee")
                        .email("employee@autonova.com")
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

        when(bffService.getEmployeeDashboard(anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(reactor.core.publisher.Mono.just(mockResponse));

        // When/Then
        mockMvc.perform(get("/api/employee/dashboard")
                        .requestAttr("userId", 123L)
                        .requestAttr("userRole", "EMPLOYEE")
                        .header("Authorization", "Bearer test-token")
                        .with(csrf()))
                .andExpect(status().isOk());
    }
}
