package com.autonova.employee_dashboard_service.service;

import com.autonova.employee_dashboard_service.dto.EmployeeDashboardResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

/**
 * BFF Service - Backend For Frontend
 * Returns lightweight dashboard information without calling downstream services
 */
@Slf4j
@Service
public class EmployeeDashboardBFFService {

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

                return Mono.fromSupplier(() -> EmployeeDashboardResponse.builder()
                                .employeeInfo(getEmployeeInfo(userId, userEmail, userRole))
                                .stats(getDashboardStats(userId))
                                .recentActivities(getRecentActivities(userId))
                                .upcomingTasks(getUpcomingTasks(userId))
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
         * Placeholder for future upcoming tasks integration.
         */
        private List<EmployeeDashboardResponse.UpcomingTask> getUpcomingTasks(Long userId) {
                log.debug("No upcoming tasks available for user: {}", userId);
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
