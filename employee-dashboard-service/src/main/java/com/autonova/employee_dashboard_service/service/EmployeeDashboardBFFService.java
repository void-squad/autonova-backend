package com.autonova.employee_dashboard_service.service;

import com.autonova.employee_dashboard_service.dto.EmployeeDashboardResponse;
import com.autonova.employee_dashboard_service.dto.project.ProjectDto;
import com.autonova.employee_dashboard_service.dto.task.TaskListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * BFF Service - Backend For Frontend
 * Aggregates data from multiple microservices for the employee dashboard
 * This service makes parallel calls to multiple services and combines the results
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeDashboardBFFService {

    private final WebClient.Builder webClientBuilder;
    private final ProjectServiceClient projectServiceClient;

    /**
     * Main method to aggregate all dashboard data from multiple microservices
     * Makes parallel calls to different services and combines the results
     * 
     * @param userId User ID from JWT token
     * @param userEmail User email from JWT token
     * @param userRole User role from JWT token
     * @param authorizationHeader Authorization header to forward to downstream services
     * @return Complete dashboard data
     */
    public Mono<EmployeeDashboardResponse> getEmployeeDashboard(Long userId, String userEmail, String userRole, String authorizationHeader) {
        log.info("Fetching aggregated dashboard data for user: {} (ID: {})", userEmail, userId);

        String userIdString = userId.toString();

        // Make parallel calls to different services
        Mono<EmployeeDashboardResponse.EmployeeInfo> employeeInfoMono = 
            Mono.just(getEmployeeInfo(userId, userEmail, userRole));
        
        Mono<List<ProjectDto>> projectsMono = 
            projectServiceClient.getProjectsByAssignee(userIdString, true, 1, 20, authorizationHeader);
        
        Mono<TaskListResponse> tasksMono = 
            projectServiceClient.getTasksByAssignee(userIdString, "InProgress", 1, 50, authorizationHeader);
        
        Mono<List<EmployeeDashboardResponse.RecentActivity>> activitiesMono = 
            Mono.just(getRecentActivities(userId));
        
        // TODO: Add calls to other services when they're ready:
        // - Time logging service
        // - Notification service
        // - Appointment service
        
        // Combine all results
        return Mono.zip(employeeInfoMono, projectsMono, tasksMono, activitiesMono)
                .map(tuple -> {
                    EmployeeDashboardResponse.EmployeeInfo employeeInfo = tuple.getT1();
                    List<ProjectDto> projects = tuple.getT2();
                    TaskListResponse tasksResponse = tuple.getT3();
                    List<EmployeeDashboardResponse.RecentActivity> activities = tuple.getT4();

                    // Convert projects to project summaries
                    List<EmployeeDashboardResponse.ProjectSummary> projectSummaries = 
                        convertToProjectSummaries(projects);

                    // Convert tasks to upcoming tasks
                    List<EmployeeDashboardResponse.UpcomingTask> upcomingTasks = 
                        convertToUpcomingTasks(tasksResponse.getItems());

                    // Calculate stats from real data
                    EmployeeDashboardResponse.DashboardStats stats = 
                        calculateStats(projects, tasksResponse);

                    return EmployeeDashboardResponse.builder()
                            .employeeInfo(employeeInfo)
                            .stats(stats)
                            .recentActivities(activities)
                            .upcomingTasks(upcomingTasks)
                            .activeProjects(projectSummaries)
                            .build();
                })
                .doOnSuccess(response -> log.info("Successfully aggregated dashboard data for user: {}", userId))
                .doOnError(error -> log.error("Error aggregating dashboard data for user {}: {}", userId, error.getMessage()))
                .onErrorResume(error -> {
                    // Fallback to partial data if some services fail
                    log.warn("Returning partial dashboard data due to error: {}", error.getMessage());
                    return Mono.just(EmployeeDashboardResponse.builder()
                            .employeeInfo(getEmployeeInfo(userId, userEmail, userRole))
                            .stats(getDashboardStats(userId))
                            .recentActivities(getRecentActivities(userId))
                            .upcomingTasks(getUpcomingTasks(userId))
                            .activeProjects(getActiveProjects(userId))
                            .build());
                });
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
     * Calculate dashboard statistics from real data
     */
    private EmployeeDashboardResponse.DashboardStats calculateStats(List<ProjectDto> projects, TaskListResponse tasksResponse) {
        log.debug("Calculating dashboard stats from real data");
        
        int activeProjects = (int) projects.stream()
                .filter(p -> "InProgress".equalsIgnoreCase(p.getStatus()) || 
                            "PendingApproval".equalsIgnoreCase(p.getStatus()))
                .count();
        
        int completedTasks = (int) tasksResponse.getItems().stream()
                .filter(t -> "Completed".equalsIgnoreCase(t.getStatus()))
                .count();
        
        return EmployeeDashboardResponse.DashboardStats.builder()
                .totalActiveProjects(activeProjects)
                .pendingAppointments(0) // TODO: Get from appointment service
                .completedTasksThisWeek(completedTasks)
                .totalRevenueThisMonth(0.0) // TODO: Get from payment service
                .totalCustomers(0) // TODO: Get from customer service
                .build();
    }

    /**
     * Get dashboard statistics (fallback with mock data)
     */
    private EmployeeDashboardResponse.DashboardStats getDashboardStats(Long userId) {
        log.debug("Fetching dashboard stats for user: {}", userId);
        
        // Mock data for fallback
        return EmployeeDashboardResponse.DashboardStats.builder()
                .totalActiveProjects(0)
                .pendingAppointments(0)
                .completedTasksThisWeek(0)
                .totalRevenueThisMonth(0.0)
                .totalCustomers(0)
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
     * Convert ProjectDto list to ProjectSummary list
     */
    private List<EmployeeDashboardResponse.ProjectSummary> convertToProjectSummaries(List<ProjectDto> projects) {
        return projects.stream()
                .map(project -> EmployeeDashboardResponse.ProjectSummary.builder()
                        .projectId(project.getProjectId())
                        .projectName(project.getTitle())
                        .customerName("Customer") // TODO: Get from customer service
                        .status(project.getStatus())
                        .startDate(project.getCreatedAt() != null ? 
                                  project.getCreatedAt().toLocalDate().toString() : null)
                        .expectedCompletionDate(project.getDueDate() != null ? 
                                               project.getDueDate().toString() : null)
                        .progressPercentage(calculateProjectProgress(project))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Convert TaskDto list to UpcomingTask list
     */
    private List<EmployeeDashboardResponse.UpcomingTask> convertToUpcomingTasks(List<com.autonova.employee_dashboard_service.dto.task.TaskDto> tasks) {
        return tasks.stream()
                .limit(10) // Only show top 10 upcoming tasks
                .map(task -> EmployeeDashboardResponse.UpcomingTask.builder()
                        .id(task.getTaskId())
                        .title(task.getTitle())
                        .description(task.getDescription())
                        .dueDate("TBD") // TODO: Get due date from task
                        .priority("MEDIUM") // TODO: Get priority from task
                        .projectId(task.getProjectId())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Calculate project progress percentage based on completed tasks
     */
    private int calculateProjectProgress(ProjectDto project) {
        if (project.getTasks() == null || project.getTasks().isEmpty()) {
            return 0;
        }
        
        long totalTasks = project.getTasks().size();
        long completedTasks = project.getTasks().stream()
                .filter(task -> "Completed".equalsIgnoreCase(task.getStatus()))
                .count();
        
        return (int) ((completedTasks * 100) / totalTasks);
    }

    /**
     * TODO: Call time-logging service
     * Will be implemented when time-logging-service is ready
     */
    private void callTimeLoggingService(Long userId, String token) {
        // Future implementation
    }

    /**
     * TODO: Call notification service
     * Will be implemented when notification-service is ready
     */
    private void callNotificationService(Long userId, String token) {
        // Future implementation
    }

    /**
     * TODO: Call appointment service
     * Will be implemented when appointment-service is ready
     */
    private void callAppointmentService(Long userId, String token) {
        // Future implementation
    }

    /**
     * TODO: Call payment/billing service
     * Will be implemented when payment-service is ready
     */
    private void callPaymentService(Long userId, String token) {
        // Future implementation
    }
}
