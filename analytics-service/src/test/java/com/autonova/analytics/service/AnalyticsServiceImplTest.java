package com.autonova.analytics.service;

import com.autonova.analytics.dto.ActivityDto;
import com.autonova.analytics.dto.DashboardAnalyticsDto;
import com.autonova.analytics.dto.EmployeePerformanceDto;
import com.autonova.analytics.dto.StatsDto;
import com.autonova.analytics.entity.Activity;
import com.autonova.analytics.entity.EmployeePerformance;
import com.autonova.analytics.repository.ActivityRepository;
import com.autonova.analytics.repository.EmployeePerformanceRepository;
import com.autonova.analytics.service.impl.AnalyticsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {

    @Mock
    private EmployeePerformanceRepository employeePerformanceRepository;

    @Mock
    private ActivityRepository activityRepository;

    @InjectMocks
    private AnalyticsServiceImpl analyticsService;

    @BeforeEach
    void setUp() {
        // Set service URLs to avoid null pointer exceptions
        ReflectionTestUtils.setField(analyticsService, "customerServiceUrl", "http://localhost:8081");
        ReflectionTestUtils.setField(analyticsService, "appointmentServiceUrl", "http://localhost:8082");
        ReflectionTestUtils.setField(analyticsService, "paymentServiceUrl", "http://localhost:8083");
        ReflectionTestUtils.setField(analyticsService, "projectServiceUrl", "http://localhost:8084");
    }

    @Test
    void getDashboardData_returnsCompleteData_whenRepositoriesReturnData() {
        // Given
        List<Activity> activities = Arrays.asList(
                createActivity(1L, "appointment", "New appointment scheduled", LocalDateTime.now().minusMinutes(5)),
                createActivity(2L, "payment", "Payment received", LocalDateTime.now().minusHours(1))
        );
        
        List<EmployeePerformance> employees = Arrays.asList(
                createEmployee("John Doe", 50, 120, 85),
                createEmployee("Jane Smith", 45, 110, 82)
        );

        when(activityRepository.findRecentActivities()).thenReturn(activities);
        when(employeePerformanceRepository.findTopPerformers()).thenReturn(employees);

        // When
        DashboardAnalyticsDto result = analyticsService.getDashboardData();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStats()).isNotNull();
        assertThat(result.getRecentActivity()).hasSize(2);
        assertThat(result.getTopEmployees()).hasSize(2);
        
        verify(activityRepository).findRecentActivities();
        verify(employeePerformanceRepository).findTopPerformers();
    }

    @Test
    void getDashboardData_returnsEmptyLists_whenRepositoriesReturnEmpty() {
        // Given
        when(activityRepository.findRecentActivities()).thenReturn(Collections.emptyList());
        when(employeePerformanceRepository.findTopPerformers()).thenReturn(Collections.emptyList());

        // When
        DashboardAnalyticsDto result = analyticsService.getDashboardData();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRecentActivity()).isEmpty();
        assertThat(result.getTopEmployees()).isEmpty();
    }

    @Test
    void getDashboardData_limitsRecentActivityTo10() {
        // Given
        List<Activity> activities = createMultipleActivities(15);
        when(activityRepository.findRecentActivities()).thenReturn(activities);
        when(employeePerformanceRepository.findTopPerformers()).thenReturn(Collections.emptyList());

        // When
        DashboardAnalyticsDto result = analyticsService.getDashboardData();

        // Then
        assertThat(result.getRecentActivity()).hasSize(10);
    }

    @Test
    void getDashboardData_limitsTopEmployeesTo5() {
        // Given
        List<EmployeePerformance> employees = createMultipleEmployees(10);
        when(activityRepository.findRecentActivities()).thenReturn(Collections.emptyList());
        when(employeePerformanceRepository.findTopPerformers()).thenReturn(employees);

        // When
        DashboardAnalyticsDto result = analyticsService.getDashboardData();

        // Then
        assertThat(result.getTopEmployees()).hasSize(5);
    }

    @Test
    void getDashboardData_mapsActivityDtoCorrectly() {
        // Given
        Activity activity = createActivity(1L, "payment", "Payment completed", LocalDateTime.now().minusMinutes(30));
        when(activityRepository.findRecentActivities()).thenReturn(Arrays.asList(activity));
        when(employeePerformanceRepository.findTopPerformers()).thenReturn(Collections.emptyList());

        // When
        DashboardAnalyticsDto result = analyticsService.getDashboardData();

        // Then
        assertThat(result.getRecentActivity()).hasSize(1);
        ActivityDto activityDto = result.getRecentActivity().get(0);
        assertThat(activityDto.getId()).isEqualTo("1");
        assertThat(activityDto.getType()).isEqualTo("payment");
        assertThat(activityDto.getMessage()).isEqualTo("Payment completed");
        assertThat(activityDto.getTime()).contains("minute");
    }

    @Test
    void getDashboardData_mapsEmployeePerformanceDtoCorrectly() {
        // Given
        EmployeePerformance employee = createEmployee("Alice Brown", 35, 95, 88);
        when(activityRepository.findRecentActivities()).thenReturn(Collections.emptyList());
        when(employeePerformanceRepository.findTopPerformers()).thenReturn(Arrays.asList(employee));

        // When
        DashboardAnalyticsDto result = analyticsService.getDashboardData();

        // Then
        assertThat(result.getTopEmployees()).hasSize(1);
        EmployeePerformanceDto empDto = result.getTopEmployees().get(0);
        assertThat(empDto.getName()).isEqualTo("Alice Brown");
        assertThat(empDto.getTasksCompleted()).isEqualTo(35);
        assertThat(empDto.getHoursLogged()).isEqualTo(95);
        assertThat(empDto.getEfficiency()).isEqualTo(88);
    }

    @Test
    void getDashboardData_continuesWhenServiceCallsFail() {
        // Given - repositories return data even if service calls fail
        when(activityRepository.findRecentActivities()).thenReturn(Collections.emptyList());
        when(employeePerformanceRepository.findTopPerformers()).thenReturn(Collections.emptyList());

        // When
        DashboardAnalyticsDto result = analyticsService.getDashboardData();

        // Then - should still return data with zero stats
        assertThat(result).isNotNull();
        assertThat(result.getStats()).isNotNull();
        assertThat(result.getStats().getTotalCustomers()).isEqualTo(0);
        assertThat(result.getStats().getActiveAppointments()).isEqualTo(0);
        assertThat(result.getStats().getMonthlyRevenue()).isEqualTo(0.0);
        assertThat(result.getStats().getActiveProjects()).isEqualTo(0);
    }

    // Helper methods
    private Activity createActivity(Long id, String type, String message, LocalDateTime timestamp) {
        Activity activity = new Activity();
        activity.setId(id);
        activity.setType(type);
        activity.setMessage(message);
        activity.setTimestamp(timestamp);
        return activity;
    }

    private EmployeePerformance createEmployee(String name, int tasks, int hours, int efficiency) {
        EmployeePerformance emp = new EmployeePerformance();
        emp.setEmployeeName(name);
        emp.setTasksCompleted(tasks);
        emp.setHoursLogged(hours);
        emp.setEfficiency(efficiency);
        return emp;
    }

    private List<Activity> createMultipleActivities(int count) {
        List<Activity> activities = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            activities.add(createActivity((long) i, "type" + i, "Message " + i, LocalDateTime.now().minusMinutes(i)));
        }
        return activities;
    }

    private List<EmployeePerformance> createMultipleEmployees(int count) {
        List<EmployeePerformance> employees = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            employees.add(createEmployee("Employee " + i, 30 + i, 80 + i, 75 + i));
        }
        return employees;
    }
}
