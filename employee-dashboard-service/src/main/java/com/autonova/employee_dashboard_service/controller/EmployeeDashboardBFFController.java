package com.autonova.employee_dashboard_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.autonova.employee_dashboard_service.dto.EmployeeDashboardResponse;
import com.autonova.employee_dashboard_service.dto.project.ProjectDto;
import com.autonova.employee_dashboard_service.dto.task.TaskListResponse;
import com.autonova.employee_dashboard_service.security.JwtService;
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
@RequestMapping("/api/employee/dashboard")
@RequiredArgsConstructor
public class EmployeeDashboardBFFController {

    private final EmployeeDashboardBFFService bffService;
    private final ProjectServiceClient projectServiceClient;
    private final JwtService jwtService;

    /**
     * Main dashboard endpoint - aggregates all employee dashboard data
     * Security: Only accessible by users with EMPLOYEE role
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
    @PreAuthorize("hasRole('EMPLOYEE')")
    public Mono<ResponseEntity<EmployeeDashboardResponse>> getEmployeeDashboard(
            Authentication authentication,
            HttpServletRequest request
    ) {
        String username = authentication.getName();
        Long userId = (Long) request.getAttribute("userId");
        String userRole = (String) request.getAttribute("userRole");
        
        // Extract JWT token for service-to-service calls
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.error("No valid Authorization header found");
            return Mono.just(ResponseEntity.badRequest().build());
        }
        
        String token = authHeader.substring(7); // Remove "Bearer " prefix

        log.info("Dashboard request from employee: {} (ID: {}, Role: {})", username, userId, userRole);

        return bffService.getEmployeeDashboard(userId, username, userRole, token)
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
    @PreAuthorize("hasRole('EMPLOYEE')")
    public Mono<ResponseEntity<List<ProjectDto>>> getMyProjects(
            @RequestParam(defaultValue = "true") boolean includeTasks,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request
    ) {
        // Extract JWT token from Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.error("No valid Authorization header found");
            return Mono.just(ResponseEntity.badRequest().build());
        }
        
        String token = authHeader.substring(7); // Remove "Bearer " prefix
        Long userId = jwtService.extractUserId(token);
        
        if (userId == null) {
            log.error("Could not extract userId from token");
            return Mono.just(ResponseEntity.badRequest().build());
        }
        
        log.info("Fetching projects for user ID: {}", userId);
        
        // Convert Long userId to UUID string for the project service
        String userIdString = userId.toString();
        
        return projectServiceClient.getProjectsByAssignee(userIdString, includeTasks, page, pageSize, token)
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
    @PreAuthorize("hasRole('EMPLOYEE')")
    public Mono<ResponseEntity<TaskListResponse>> getMyTasks(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize,
            HttpServletRequest request
    ) {
        // Extract JWT token from Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.error("No valid Authorization header found");
            return Mono.just(ResponseEntity.badRequest().build());
        }
        
        String token = authHeader.substring(7); // Remove "Bearer " prefix
        Long userId = jwtService.extractUserId(token);
        
        if (userId == null) {
            log.error("Could not extract userId from token");
            return Mono.just(ResponseEntity.badRequest().build());
        }
        
        log.info("Fetching tasks for user ID: {} with status: {}", userId, status);
        
        // Convert Long userId to UUID string for the project service
        String userIdString = userId.toString();
        
        return projectServiceClient.getTasksByAssignee(userIdString, status, page, pageSize, token)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("Error fetching tasks: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }
}
