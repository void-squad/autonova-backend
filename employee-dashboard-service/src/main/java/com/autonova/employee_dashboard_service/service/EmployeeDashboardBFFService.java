package com.autonova.employee_dashboard_service.service;

import com.autonova.employee_dashboard_service.dto.EmployeeDashboardResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * BFF Service - Backend For Frontend
 * Aggregates data from multiple microservices for the employee dashboard
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeDashboardBFFService {

    private final WebClient.Builder webClientBuilder;

    /**
     * Main method to aggregate all dashboard data
     * Currently returns mock data until services are implemented
     */
    public EmployeeDashboardResponse getEmployeeDashboard(Long userId, String userEmail, String userRole) {
        log.info("Fetching dashboard data for user: {} (ID: {})", userEmail, userId);

        // In the future, these will be parallel calls to different services
        EmployeeDashboardResponse.EmployeeInfo employeeInfo = getEmployeeInfo(userId, userEmail, userRole);
        EmployeeDashboardResponse.DashboardStats stats = getDashboardStats(userId);
        List<EmployeeDashboardResponse.RecentActivity> activities = getRecentActivities(userId);
        List<EmployeeDashboardResponse.UpcomingTask> tasks = getUpcomingTasks(userId);
        List<EmployeeDashboardResponse.ProjectSummary> projects = getActiveProjects(userId);

        return EmployeeDashboardResponse.builder()
                .employeeInfo(employeeInfo)
                .stats(stats)
                .recentActivities(activities)
                .upcomingTasks(tasks)
                .activeProjects(projects)
                .build();
    }

    /**
     * Get employee information
     * TODO: Call auth-service or employee-service when available
     */
    private EmployeeDashboardResponse.EmployeeInfo getEmployeeInfo(Long userId, String email, String role) {
        log.debug("Fetching employee info for user: {}", userId);
        
        // Mock data for now
        return EmployeeDashboardResponse.EmployeeInfo.builder()
                .userId(userId)
                .name("Employee User") // TODO: Get from auth service
                .email(email)
                .role(role)
                .department("Service Department")
                .build();
    }

    /**
     * Get dashboard statistics
     * TODO: Aggregate from project-service, appointment-service, payment-service, customer-service
     */
    private EmployeeDashboardResponse.DashboardStats getDashboardStats(Long userId) {
        log.debug("Fetching dashboard stats for user: {}", userId);
        
        // Mock data for now
        return EmployeeDashboardResponse.DashboardStats.builder()
                .totalActiveProjects(5)
                .pendingAppointments(3)
                .completedTasksThisWeek(12)
                .totalRevenueThisMonth(45000.00)
                .totalCustomers(28)
                .build();
    }

    /**
     * Get recent activities
     * TODO: Call activity/audit service or aggregate from multiple services
     */
    private List<EmployeeDashboardResponse.RecentActivity> getRecentActivities(Long userId) {
        log.debug("Fetching recent activities for user: {}", userId);
        
        List<EmployeeDashboardResponse.RecentActivity> activities = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        // Mock data
        activities.add(EmployeeDashboardResponse.RecentActivity.builder()
                .id("ACT-001")
                .type("PROJECT_UPDATE")
                .description("Updated project PRJ-2024-001 progress to 65%")
                .timestamp(LocalDateTime.now().minusHours(2).format(formatter))
                .status("COMPLETED")
                .build());
        
        activities.add(EmployeeDashboardResponse.RecentActivity.builder()
                .id("ACT-002")
                .type("APPOINTMENT")
                .description("Completed appointment with customer John Doe")
                .timestamp(LocalDateTime.now().minusHours(5).format(formatter))
                .status("COMPLETED")
                .build());
        
        activities.add(EmployeeDashboardResponse.RecentActivity.builder()
                .id("ACT-003")
                .type("PAYMENT_RECEIVED")
                .description("Payment received for Invoice #INV-2024-045")
                .timestamp(LocalDateTime.now().minusDays(1).format(formatter))
                .status("COMPLETED")
                .build());
        
        return activities;
    }

    /**
     * Get upcoming tasks
     * TODO: Call project-service or task-management service
     */
    private List<EmployeeDashboardResponse.UpcomingTask> getUpcomingTasks(Long userId) {
        log.debug("Fetching upcoming tasks for user: {}", userId);
        
        List<EmployeeDashboardResponse.UpcomingTask> tasks = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        // Mock data
        tasks.add(EmployeeDashboardResponse.UpcomingTask.builder()
                .id("TASK-001")
                .title("Complete vehicle inspection")
                .description("Inspect vehicle for project PRJ-2024-001")
                .dueDate(LocalDateTime.now().plusDays(2).format(formatter))
                .priority("HIGH")
                .projectId("PRJ-2024-001")
                .build());
        
        tasks.add(EmployeeDashboardResponse.UpcomingTask.builder()
                .id("TASK-002")
                .title("Customer follow-up call")
                .description("Follow up with customer regarding service feedback")
                .dueDate(LocalDateTime.now().plusDays(3).format(formatter))
                .priority("MEDIUM")
                .projectId("PRJ-2024-003")
                .build());
        
        tasks.add(EmployeeDashboardResponse.UpcomingTask.builder()
                .id("TASK-003")
                .title("Submit weekly report")
                .description("Prepare and submit weekly progress report")
                .dueDate(LocalDateTime.now().plusDays(5).format(formatter))
                .priority("MEDIUM")
                .projectId(null)
                .build());
        
        return tasks;
    }

    /**
     * Get active projects
     * TODO: Call project-service
     */
    private List<EmployeeDashboardResponse.ProjectSummary> getActiveProjects(Long userId) {
        log.debug("Fetching active projects for user: {}", userId);
        
        List<EmployeeDashboardResponse.ProjectSummary> projects = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        // Mock data
        projects.add(EmployeeDashboardResponse.ProjectSummary.builder()
                .projectId("PRJ-2024-001")
                .projectName("Toyota Camry - Full Service")
                .customerName("John Doe")
                .status("IN_PROGRESS")
                .startDate(LocalDateTime.now().minusDays(10).format(formatter))
                .expectedCompletionDate(LocalDateTime.now().plusDays(5).format(formatter))
                .progressPercentage(65)
                .build());
        
        projects.add(EmployeeDashboardResponse.ProjectSummary.builder()
                .projectId("PRJ-2024-003")
                .projectName("Honda Accord - Repair")
                .customerName("Jane Smith")
                .status("IN_PROGRESS")
                .startDate(LocalDateTime.now().minusDays(5).format(formatter))
                .expectedCompletionDate(LocalDateTime.now().plusDays(10).format(formatter))
                .progressPercentage(30)
                .build());
        
        projects.add(EmployeeDashboardResponse.ProjectSummary.builder()
                .projectId("PRJ-2024-005")
                .projectName("BMW X5 - Paint Job")
                .customerName("Robert Johnson")
                .status("PENDING")
                .startDate(LocalDateTime.now().plusDays(2).format(formatter))
                .expectedCompletionDate(LocalDateTime.now().plusDays(20).format(formatter))
                .progressPercentage(0)
                .build());
        
        return projects;
    }

    /**
     * Future method to call project service
     * Will be implemented when project-service endpoints are available
     */
    private void callProjectService(Long userId) {
        // Example WebClient call (commented out for now)
        /*
        WebClient webClient = webClientBuilder
                .baseUrl("http://project-service")
                .build();
        
        return webClient.get()
                .uri("/api/projects/employee/{userId}", userId)
                .retrieve()
                .bodyToMono(ProjectResponse.class)
                .block();
        */
    }

    /**
     * Future method to call customer service
     */
    private void callCustomerService(Long userId) {
        // Will be implemented when customer-service is ready
    }

    /**
     * Future method to call appointment service
     */
    private void callAppointmentService(Long userId) {
        // Will be implemented when appointment-service is ready
    }

    /**
     * Future method to call payment service
     */
    private void callPaymentService(Long userId) {
        // Will be implemented when payment-service is ready
    }
}
