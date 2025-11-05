package com.autonova.employee_dashboard_service.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import com.autonova.employee_dashboard_service.dto.SaveReportRequest;
import com.autonova.employee_dashboard_service.dto.SaveReportResponse;
import com.autonova.employee_dashboard_service.entity.SavedAnalyticsReport;
import com.autonova.employee_dashboard_service.repository.SavedAnalyticsReportRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final SavedAnalyticsReportRepository reportRepository;
    private final WebClient.Builder webClientBuilder;

    @Value("${services.analytics-reporting.url}")
    private String analyticsServiceUrl;

    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getAnalyticsSummary(Long employeeId) {
        log.info("Fetching analytics summary for employee: {}", employeeId);
        
        return webClientBuilder.build()
                .get()
                .uri(analyticsServiceUrl + "/api/analytics/summary?employeeId=" + employeeId)
                .retrieve()
                .bodyToMono(Map.class)
                .map(map -> (Map<String, Object>) map)
                .doOnError(error -> log.error("Error fetching analytics summary: {}", error.getMessage()));
    }

    @Transactional
    public SaveReportResponse saveReport(Long employeeId, SaveReportRequest request) {
        log.info("Saving analytics report for employee: {}", employeeId);

        SavedAnalyticsReport report = SavedAnalyticsReport.builder()
                .employeeId(employeeId)
                .reportName(request.getReportName())
                .reportParameters(request.getReportParameters())
                .build();

        SavedAnalyticsReport saved = reportRepository.save(report);

        return SaveReportResponse.builder()
                .reportId(saved.getReportId())
                .employeeId(saved.getEmployeeId())
                .reportName(saved.getReportName())
                .reportParameters(saved.getReportParameters())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<SaveReportResponse> getSavedReports(Long employeeId) {
        log.info("Fetching saved reports for employee: {}", employeeId);

        return reportRepository.findByEmployeeId(employeeId)
                .stream()
                .map(report -> SaveReportResponse.builder()
                        .reportId(report.getReportId())
                        .employeeId(report.getEmployeeId())
                        .reportName(report.getReportName())
                        .reportParameters(report.getReportParameters())
                        .createdAt(report.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}
