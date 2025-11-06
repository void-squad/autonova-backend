package com.autonova.employee_dashboard_service.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.autonova.employee_dashboard_service.dto.SaveReportRequest;
import com.autonova.employee_dashboard_service.dto.SaveReportResponse;
import com.autonova.employee_dashboard_service.service.AnalyticsService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/dashboard/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/summary")
    public Mono<ResponseEntity<Map<String, Object>>> getAnalyticsSummary(
            @RequestHeader(value = "X-Employee-Id", defaultValue = "1") Long employeeId) {
        return analyticsService.getAnalyticsSummary(employeeId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping("/save-report")
    public ResponseEntity<SaveReportResponse> saveReport(
            @RequestHeader(value = "X-Employee-Id", defaultValue = "1") Long employeeId,
            @RequestBody SaveReportRequest request) {
        SaveReportResponse response = analyticsService.saveReport(employeeId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/saved-reports")
    public ResponseEntity<List<SaveReportResponse>> getSavedReports(
            @RequestHeader(value = "X-Employee-Id", defaultValue = "1") Long employeeId) {
        List<SaveReportResponse> reports = analyticsService.getSavedReports(employeeId);
        return ResponseEntity.ok(reports);
    }

    // TODO: Re-enable authentication
    // private Long extractEmployeeId(Authentication authentication) {
    //     String username = authentication.getName();
    //     return Long.parseLong(username);
    // }
}
