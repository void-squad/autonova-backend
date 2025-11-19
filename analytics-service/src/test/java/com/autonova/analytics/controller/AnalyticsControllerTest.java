package com.autonova.analytics.controller;

import com.autonova.analytics.dto.ActivityDto;
import com.autonova.analytics.dto.DashboardAnalyticsDto;
import com.autonova.analytics.dto.EmployeePerformanceDto;
import com.autonova.analytics.dto.StatsDto;
import com.autonova.analytics.service.AnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnalyticsController.class)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @Test
    void getDashboardAnalytics_returnsOk_whenDataIsAvailable() throws Exception {
        // Given
        StatsDto stats = new StatsDto(100L, 25L, 5000.0, 15L);
        ActivityDto activity = new ActivityDto("1", "appointment", "New appointment", "5 minutes ago");
        EmployeePerformanceDto employee = new EmployeePerformanceDto("John Doe", 50, 120, 85);
        
        DashboardAnalyticsDto dashboardData = new DashboardAnalyticsDto(
                stats,
                Arrays.asList(activity),
                Arrays.asList(employee)
        );

        when(analyticsService.getDashboardData()).thenReturn(dashboardData);

        // When & Then
        mockMvc.perform(get("/api/analytics/dashboard")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats").exists())
                .andExpect(jsonPath("$.stats.totalCustomers").value(100))
                .andExpect(jsonPath("$.stats.activeAppointments").value(25))
                .andExpect(jsonPath("$.stats.monthlyRevenue").value(5000.0))
                .andExpect(jsonPath("$.stats.activeProjects").value(15))
                .andExpect(jsonPath("$.recentActivity").isArray())
                .andExpect(jsonPath("$.recentActivity[0].id").value("1"))
                .andExpect(jsonPath("$.recentActivity[0].type").value("appointment"))
                .andExpect(jsonPath("$.topEmployees").isArray())
                .andExpect(jsonPath("$.topEmployees[0].name").value("John Doe"))
                .andExpect(jsonPath("$.topEmployees[0].tasksCompleted").value(50));

        verify(analyticsService).getDashboardData();
    }

    @Test
    void getDashboardAnalytics_returnsEmptyLists_whenNoDataAvailable() throws Exception {
        // Given
        StatsDto stats = new StatsDto(0L, 0L, 0.0, 0L);
        DashboardAnalyticsDto dashboardData = new DashboardAnalyticsDto(
                stats,
                Collections.emptyList(),
                Collections.emptyList()
        );

        when(analyticsService.getDashboardData()).thenReturn(dashboardData);

        // When & Then
        mockMvc.perform(get("/api/analytics/dashboard")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.totalCustomers").value(0))
                .andExpect(jsonPath("$.recentActivity").isEmpty())
                .andExpect(jsonPath("$.topEmployees").isEmpty());

        verify(analyticsService).getDashboardData();
    }

    @Test
    void getDashboardAnalytics_returnsMultipleActivities_whenAvailable() throws Exception {
        // Given
        StatsDto stats = new StatsDto(50L, 10L, 2500.0, 8L);
        ActivityDto activity1 = new ActivityDto("1", "appointment", "Appointment scheduled", "5 minutes ago");
        ActivityDto activity2 = new ActivityDto("2", "payment", "Payment received", "10 minutes ago");
        ActivityDto activity3 = new ActivityDto("3", "project", "Project started", "1 hour ago");
        
        DashboardAnalyticsDto dashboardData = new DashboardAnalyticsDto(
                stats,
                Arrays.asList(activity1, activity2, activity3),
                Collections.emptyList()
        );

        when(analyticsService.getDashboardData()).thenReturn(dashboardData);

        // When & Then
        mockMvc.perform(get("/api/analytics/dashboard")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recentActivity").isArray())
                .andExpect(jsonPath("$.recentActivity.length()").value(3))
                .andExpect(jsonPath("$.recentActivity[0].type").value("appointment"))
                .andExpect(jsonPath("$.recentActivity[1].type").value("payment"))
                .andExpect(jsonPath("$.recentActivity[2].type").value("project"));

        verify(analyticsService).getDashboardData();
    }

    @Test
    void getDashboardAnalytics_returnsMultipleEmployees_whenAvailable() throws Exception {
        // Given
        StatsDto stats = new StatsDto(30L, 5L, 1500.0, 4L);
        EmployeePerformanceDto emp1 = new EmployeePerformanceDto("Alice", 60, 150, 90);
        EmployeePerformanceDto emp2 = new EmployeePerformanceDto("Bob", 55, 140, 88);
        EmployeePerformanceDto emp3 = new EmployeePerformanceDto("Charlie", 50, 130, 85);
        
        DashboardAnalyticsDto dashboardData = new DashboardAnalyticsDto(
                stats,
                Collections.emptyList(),
                Arrays.asList(emp1, emp2, emp3)
        );

        when(analyticsService.getDashboardData()).thenReturn(dashboardData);

        // When & Then
        mockMvc.perform(get("/api/analytics/dashboard")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topEmployees").isArray())
                .andExpect(jsonPath("$.topEmployees.length()").value(3))
                .andExpect(jsonPath("$.topEmployees[0].name").value("Alice"))
                .andExpect(jsonPath("$.topEmployees[0].efficiency").value(90))
                .andExpect(jsonPath("$.topEmployees[1].name").value("Bob"))
                .andExpect(jsonPath("$.topEmployees[2].name").value("Charlie"));

        verify(analyticsService).getDashboardData();
    }

    @Test
    void healthCheck_returnsOk() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/analytics/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Analytics Service is running"));
    }
}
