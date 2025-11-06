package com.autonova.employee_dashboard_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
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
    public Mono<ResponseEntity<OperationalViewResponse>> getOperationalView(
            @RequestHeader(value = "X-Employee-Id", defaultValue = "1") Long employeeId) {
        return operationalDashboardService.getOperationalView(employeeId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    // TODO: Re-enable authentication
    // private Long extractEmployeeId(Authentication authentication) {
    //     String username = authentication.getName();
    //     return Long.parseLong(username);
    // }
}
