package com.autonova.employee_dashboard_service.service;

import com.autonova.employee_dashboard_service.dto.EmployeeDashboardResponse;
import com.autonova.employee_dashboard_service.dto.task.TaskDto;
import com.autonova.employee_dashboard_service.dto.task.TaskListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * BFF Service - Backend For Frontend
 * Returns lightweight dashboard information without calling downstream services
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeDashboardBFFService {

        private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

        private final ProjectServiceClient projectServiceClient;

        /**
         * Main method to aggregate all dashboard data.
         * Currently returns lightweight data without reaching external services.
         */
        public Mono<EmployeeDashboardResponse> getEmployeeDashboard(Long userId, String userEmail, String userRole, String authorizationHeader) {
                Objects.requireNonNull(userId, "userId is required");
                log.info("Returning lightweight dashboard data for user: {} (ID: {})", userEmail, userId);
                if (log.isDebugEnabled()) {
                        log.debug("Authorization header present for user {}: {}", userId, StringUtils.hasText(authorizationHeader));
                }

                return fetchUpcomingTasks(userId, authorizationHeader)
                                .map(upcomingTasks -> EmployeeDashboardResponse.builder()
                                                .employeeInfo(getEmployeeInfo(userId, userEmail, userRole))
                                                .stats(getDashboardStats(userId))
                                                .recentActivities(getRecentActivities(userId))
                                                .upcomingTasks(upcomingTasks)
                                                .activeProjects(getActiveProjects(userId))
                                                .build())
                                .doOnSuccess(response -> log.info("Successfully prepared dashboard data for user: {}", userId))
                                .doOnError(error -> log.error("Error preparing dashboard data for user {}: {}", userId, error.getMessage()))
                                .onErrorResume(error -> {
                                        log.warn("Returning minimal dashboard data after error: {}", error.getMessage());
                                        return Mono.just(EmployeeDashboardResponse.builder()
                                                        .employeeInfo(getEmployeeInfo(userId, userEmail, userRole))
                                                        .stats(getDashboardStats(userId))
                                                        .recentActivities(List.of())
                                                        .upcomingTasks(List.of())
                                                        .activeProjects(List.of())
                                                        .build());
                                });
        }
        private Mono<List<EmployeeDashboardResponse.UpcomingTask>> fetchUpcomingTasks(Long userId, String authorizationHeader) {
                if (!StringUtils.hasText(authorizationHeader)) {
                        log.warn("Missing authorization header while fetching tasks for user {}", userId);
                        return Mono.just(List.of());
                }

                return projectServiceClient.getTasksByAssignee(String.valueOf(userId), null, 1, 50, authorizationHeader)
                                .map(response -> {
                                        if (response == null || response.getItems() == null) {
                                                return List.<EmployeeDashboardResponse.UpcomingTask>of();
                                        }

                                        return response.getItems().stream()
                                                        .sorted(Comparator.comparing((TaskDto task) -> Optional.ofNullable(task.getScheduledStart()).orElse(task.getCreatedAt()))
                                                                        .thenComparing(TaskDto::getTitle, Comparator.nullsLast(String::compareToIgnoreCase)))
                                                        .limit(10)
                                                        .map(this::mapToUpcomingTask)
                                                        .collect(Collectors.toList());
                                })
                                .onErrorResume(error -> {
                                        log.warn("Falling back to empty task list for user {} due to downstream error: {}", userId, error.getMessage());
                                        return Mono.just(List.of());
                                });
        }

        private EmployeeDashboardResponse.UpcomingTask mapToUpcomingTask(TaskDto task) {
                return EmployeeDashboardResponse.UpcomingTask.builder()
                                .id(task.getTaskId())
                                .title(task.getTitle())
                                .description(task.getDescription())
                                .dueDate(formatDate(task.getScheduledEnd(), task.getScheduledStart()))
                                .priority(task.getStatus())
                                .projectId(resolveProjectId(task))
                                .build();
        }

        private String resolveProjectId(TaskDto task) {
                if (StringUtils.hasText(task.getProjectId())) {
                        return task.getProjectId();
                }
                return task.getProject() != null ? task.getProject().getProjectId() : null;
        }

        private String formatDate(OffsetDateTime primary, OffsetDateTime fallback) {
                OffsetDateTime value = primary != null ? primary : fallback;
                return value != null ? ISO_FORMATTER.format(value) : null;
        }

        /**
         * Get employee information
         * TODO: Call auth-service or employee-service when available
         */
        private EmployeeDashboardResponse.EmployeeInfo getEmployeeInfo(Long userId, String email, String role) {
                log.debug("Fetching employee info for user: {}", userId);
        
                return EmployeeDashboardResponse.EmployeeInfo.builder()
                                .userId(userId)
                                .name("Employee User") // TODO: Get from auth service
                                .email(email)
                                .role(role)
                                .department("Service Department")
                                .build();
        }

        /**
         * Get dashboard statistics (currently zeroed until downstream services are wired)
         */
        private EmployeeDashboardResponse.DashboardStats getDashboardStats(Long userId) {
                log.debug("Fetching dashboard stats for user: {}", userId);
        
                return EmployeeDashboardResponse.DashboardStats.builder()
                                .totalActiveProjects(0)
                                .pendingAppointments(0)
                                .completedTasksThisWeek(0)
                                .totalRevenueThisMonth(0.0)
                                .totalCustomers(0)
                                .build();
        }

        /**
         * Placeholder for future activity feed integration.
         */
        private List<EmployeeDashboardResponse.RecentActivity> getRecentActivities(Long userId) {
                log.debug("No activity feed available for user: {}", userId);
                return List.of();
        }

        /**
         * Placeholder for future active project integration.
         */
        private List<EmployeeDashboardResponse.ProjectSummary> getActiveProjects(Long userId) {
                log.debug("No active project data available for user: {}", userId);
                return List.of();
        }
}
