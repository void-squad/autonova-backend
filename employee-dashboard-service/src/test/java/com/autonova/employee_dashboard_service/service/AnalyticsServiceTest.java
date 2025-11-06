package com.autonova.employee_dashboard_service.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.autonova.employee_dashboard_service.dto.SaveReportRequest;
import com.autonova.employee_dashboard_service.dto.SaveReportResponse;
import com.autonova.employee_dashboard_service.entity.SavedAnalyticsReport;
import com.autonova.employee_dashboard_service.repository.SavedAnalyticsReportRepository;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private SavedAnalyticsReportRepository reportRepository;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private AnalyticsService analyticsService;

    private Long employeeId;
    private String analyticsServiceUrl;

    @BeforeEach
    void setUp() {
        employeeId = 1L;
        analyticsServiceUrl = "http://localhost:8080";
        ReflectionTestUtils.setField(analyticsService, "analyticsServiceUrl", analyticsServiceUrl);
    }

    @Test
    void getAnalyticsSummary_ShouldReturnAnalyticsData() {
        // Arrange
        Map<String, Object> mockData = new HashMap<>();
        mockData.put("totalProjects", 10);
        mockData.put("completedProjects", 5);
        mockData.put("pendingProjects", 5);

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(mockData));

        // Act
        Mono<Map<String, Object>> result = analyticsService.getAnalyticsSummary(employeeId);

        // Assert
        StepVerifier.create(result)
                .assertNext(data -> {
                    assertThat(data).isNotNull();
                    assertThat(data.get("totalProjects")).isEqualTo(10);
                    assertThat(data.get("completedProjects")).isEqualTo(5);
                    assertThat(data.get("pendingProjects")).isEqualTo(5);
                })
                .verifyComplete();

        verify(webClientBuilder, times(1)).build();
    }

    @Test
    void getAnalyticsSummary_WhenServiceFails_ShouldPropagateError() {
        // Arrange
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class))
                .thenReturn(Mono.error(new RuntimeException("Service unavailable")));

        // Act
        Mono<Map<String, Object>> result = analyticsService.getAnalyticsSummary(employeeId);

        // Assert
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void saveReport_ShouldSaveAndReturnReport() {
        // Arrange
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("period", "monthly");
        
        SaveReportRequest request = SaveReportRequest.builder()
                .reportName("Monthly Report")
                .reportParameters(parameters)
                .build();

        SavedAnalyticsReport savedReport = SavedAnalyticsReport.builder()
                .reportId(1L)
                .employeeId(employeeId)
                .reportName("Monthly Report")
                .reportParameters(parameters)
                .createdAt(LocalDateTime.now())
                .build();

        when(reportRepository.save(any(SavedAnalyticsReport.class))).thenReturn(savedReport);

        // Act
        SaveReportResponse response = analyticsService.saveReport(employeeId, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getReportId()).isEqualTo(1L);
        assertThat(response.getEmployeeId()).isEqualTo(employeeId);
        assertThat(response.getReportName()).isEqualTo("Monthly Report");
        assertThat(response.getReportParameters()).isNotNull();
        assertThat(response.getReportParameters().get("period")).isEqualTo("monthly");
        assertThat(response.getCreatedAt()).isNotNull();

        verify(reportRepository, times(1)).save(any(SavedAnalyticsReport.class));
    }

    @Test
    void getSavedReports_ShouldReturnListOfReports() {
        // Arrange
        Map<String, Object> params1 = new HashMap<>();
        params1.put("period", "weekly");
        
        Map<String, Object> params2 = new HashMap<>();
        params2.put("period", "monthly");
        
        SavedAnalyticsReport report1 = SavedAnalyticsReport.builder()
                .reportId(1L)
                .employeeId(employeeId)
                .reportName("Report 1")
                .reportParameters(params1)
                .createdAt(LocalDateTime.now())
                .build();

        SavedAnalyticsReport report2 = SavedAnalyticsReport.builder()
                .reportId(2L)
                .employeeId(employeeId)
                .reportName("Report 2")
                .reportParameters(params2)
                .createdAt(LocalDateTime.now())
                .build();

        List<SavedAnalyticsReport> reports = Arrays.asList(report1, report2);

        when(reportRepository.findByEmployeeId(employeeId)).thenReturn(reports);

        // Act
        List<SaveReportResponse> result = analyticsService.getSavedReports(employeeId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getReportName()).isEqualTo("Report 1");
        assertThat(result.get(1).getReportName()).isEqualTo("Report 2");

        verify(reportRepository, times(1)).findByEmployeeId(employeeId);
    }

    @Test
    void getSavedReports_WhenNoReportsExist_ShouldReturnEmptyList() {
        // Arrange
        when(reportRepository.findByEmployeeId(employeeId)).thenReturn(Collections.emptyList());

        // Act
        List<SaveReportResponse> result = analyticsService.getSavedReports(employeeId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(reportRepository, times(1)).findByEmployeeId(employeeId);
    }

    @Test
    void saveReport_WithDifferentParameters_ShouldSaveCorrectly() {
        // Arrange
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("filter", "status=completed");
        
        SaveReportRequest request = SaveReportRequest.builder()
                .reportName("Custom Report")
                .reportParameters(parameters)
                .build();

        SavedAnalyticsReport savedReport = SavedAnalyticsReport.builder()
                .reportId(3L)
                .employeeId(employeeId)
                .reportName("Custom Report")
                .reportParameters(parameters)
                .createdAt(LocalDateTime.now())
                .build();

        when(reportRepository.save(any(SavedAnalyticsReport.class))).thenReturn(savedReport);

        // Act
        SaveReportResponse response = analyticsService.saveReport(employeeId, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getReportName()).isEqualTo("Custom Report");
        assertThat(response.getReportParameters()).isNotNull();
        assertThat(response.getReportParameters().get("filter")).isEqualTo("status=completed");

        verify(reportRepository, times(1)).save(any(SavedAnalyticsReport.class));
    }
}
