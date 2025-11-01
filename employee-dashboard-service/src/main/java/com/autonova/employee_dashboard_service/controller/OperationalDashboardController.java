package com.autonova.employee_dashboard_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.autonova.employee_dashboard_service.dto.OperationalViewResponse;
import com.autonova.employee_dashboard_service.service.OperationalDashboardService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/dashboard/operational")
@RequiredArgsConstructor
public class OperationalDashboardController {

    private final OperationalDashboardService operationalDashboardService;

    @GetMapping
    public Mono<ResponseEntity<OperationalViewResponse>> getOperationalView(Authentication authentication) {
        Long employeeId = extractEmployeeId(authentication);
        return operationalDashboardService.getOperationalView(employeeId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    private Long extractEmployeeId(Authentication authentication) {
        // Extract employee ID from authentication token
        // This is a placeholder - implement based on your auth service structure
        String username = authentication.getName();
        return Long.parseLong(username); // Adjust this based on your auth implementation
    }
}
