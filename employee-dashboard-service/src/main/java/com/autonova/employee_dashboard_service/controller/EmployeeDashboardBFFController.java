package com.autonova.employee_dashboard_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.autonova.employee_dashboard_service.dto.EmployeeDashboardResponse;
import com.autonova.employee_dashboard_service.dto.project.ProjectDto;
import com.autonova.employee_dashboard_service.dto.task.TaskListResponse;
import com.autonova.employee_dashboard_service.service.EmployeeDashboardBFFService;
import com.autonova.employee_dashboard_service.service.ProjectServiceClient;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * BFF Controller for Employee Dashboard
 * Single endpoint that aggregates data from multiple microservices
 */
@Slf4j
@RestController
@RequestMapping("/api/employee-dashboard")
@RequiredArgsConstructor
public class EmployeeDashboardBFFController {

    private final EmployeeDashboardBFFService bffService;
    private final ProjectServiceClient projectServiceClient;

    /**
     * Main dashboard endpoint - aggregates all employee dashboard data
     * 
     * This is the ONLY endpoint frontend needs to call!
     * It aggregates data from:
     * - Project Service (projects & tasks)
     * - Time Logging Service (TODO)
     * - Notification Service (TODO)
     * - Appointment Service (TODO)
     * - Payment/Billing Service (TODO)
     * 
     * @param authentication Spring Security authentication object
     * @param request HTTP request containing userId, userRole and Authorization header
     * @return Complete dashboard data with all aggregated information
     */
    @GetMapping
    public Mono<ResponseEntity<EmployeeDashboardResponse>> getEmployeeDashboard(
            Authentication authentication,
            HttpServletRequest request
    ) {
        if (authentication == null) {
            log.error("Authentication not present in security context");
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        String username = authentication.getName();
        Long userId = (Long) request.getAttribute("userId");
        String userRole = (String) request.getAttribute("userRole");

        if (userId == null || userRole == null) {
            log.error("Request context missing identity attributes; rejecting dashboard request");
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        
        String authorizationHeader = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorizationHeader)) {
            log.error("No Authorization header found on dashboard request");
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        log.info("Dashboard request from employee: {} (ID: {}, Role: {})", username, userId, userRole);

        return bffService.getEmployeeDashboard(userId, username, userRole, authorizationHeader)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("Error fetching dashboard data for user {}: {}", userId, error.getMessage(), error);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Health check endpoint for the BFF
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Employee Dashboard BFF is running");
    }

    /**
     * Get projects assigned to the authenticated user
     * 
     * @param includeTasks Whether to include tasks in each project
     * @param page Page number (default: 1)
     * @param pageSize Number of items per page (default: 20)
     * @param request HTTP request to extract JWT token
     * @return Mono of List of projects
     */
    @GetMapping("/projects")
    public Mono<ResponseEntity<List<ProjectDto>>> getMyProjects(
            @RequestParam(defaultValue = "true") boolean includeTasks,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request
    ) {
        String authorizationHeader = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorizationHeader)) {
            log.error("No Authorization header found on projects request");
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            log.error("Could not resolve userId from request context");
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        
        log.info("Fetching projects for user ID: {}", userId);
        
        // Convert Long userId to UUID string for the project service
        String userIdString = userId.toString();
        
        return projectServiceClient.getProjectsByAssignee(userIdString, includeTasks, page, pageSize, authorizationHeader)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("Error fetching projects: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Get tasks assigned to the authenticated user
     * 
     * @param status Task status filter (optional, e.g., "InProgress", "Completed")
     * @param page Page number (default: 1)
     * @param pageSize Number of items per page (default: 50)
     * @param request HTTP request to extract JWT token
     * @return Mono of TaskListResponse with pagination
     */
    @GetMapping("/tasks")
    public Mono<ResponseEntity<TaskListResponse>> getMyTasks(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize,
            HttpServletRequest request
    ) {
        String authorizationHeader = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorizationHeader)) {
            log.error("No Authorization header found on tasks request");
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            log.error("Could not resolve userId from request context");
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        
        log.info("Fetching tasks for user ID: {} with status: {}", userId, status);
        
        // Convert Long userId to UUID string for the project service
        String userIdString = userId.toString();
        
        return projectServiceClient.getTasksByAssignee(userIdString, status, page, pageSize, authorizationHeader)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("Error fetching tasks: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }
}
