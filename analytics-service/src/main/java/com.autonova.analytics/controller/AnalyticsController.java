package com.autonova.analytics.controller;

import com.autonova.analytics.dto.DashboardAnalyticsDto;
import com.autonova.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardAnalyticsDto> getDashboardAnalytics() {
        DashboardAnalyticsDto data = analyticsService.getDashboardData();
        return ResponseEntity.ok(data);
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Analytics Service is running");
    }
}