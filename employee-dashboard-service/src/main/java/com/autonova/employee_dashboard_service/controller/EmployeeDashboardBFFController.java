package com.autonova.employee_dashboard_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.autonova.employee_dashboard_service.dto.EmployeeDashboardResponse;
import com.autonova.employee_dashboard_service.service.EmployeeDashboardBFFService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

    /**
     * Main dashboard endpoint - aggregates all employee dashboard data
     * Security: Only accessible by users with EMPLOYEE role
     * 
     * @param authentication Spring Security authentication object
     * @param request HTTP request containing userId and userRole attributes
     * @return Complete dashboard data
     */
    @GetMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<EmployeeDashboardResponse> getEmployeeDashboard(
            Authentication authentication,
            HttpServletRequest request
    ) {
        String username = authentication.getName();
        Long userId = (Long) request.getAttribute("userId");
        String userRole = (String) request.getAttribute("userRole");

        log.info("Dashboard request from employee: {} (ID: {}, Role: {})", username, userId, userRole);

        try {
            EmployeeDashboardResponse dashboardData = bffService.getEmployeeDashboard(userId, username, userRole);
            return ResponseEntity.ok(dashboardData);
        } catch (Exception e) {
            log.error("Error fetching dashboard data for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Health check endpoint for the BFF
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Employee Dashboard BFF is running");
    }
}
