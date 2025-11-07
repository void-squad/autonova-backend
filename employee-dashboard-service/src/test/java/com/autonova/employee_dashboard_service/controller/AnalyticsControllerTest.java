package com.autonova.employee_dashboard_service.controller;

import com.autonova.employee_dashboard_service.dto.SaveReportRequest;
import com.autonova.employee_dashboard_service.dto.SaveReportResponse;
import com.autonova.employee_dashboard_service.service.AnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @Test
    @WithMockUser(username = "1")
    void getAnalyticsSummary_ShouldReturnSummary() throws Exception {
        Map<String, Object> summaryData = new HashMap<>();
        summaryData.put("totalProjects", 10);
        summaryData.put("completedProjects", 5);
        summaryData.put("pendingProjects", 5);

        when(analyticsService.getAnalyticsSummary(anyLong()))
                .thenReturn(Mono.just(summaryData));

        mockMvc.perform(get("/api/dashboard/analytics/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProjects").value(10))
                .andExpect(jsonPath("$.completedProjects").value(5))
                .andExpect(jsonPath("$.pendingProjects").value(5));
    }

    @Test
    @WithMockUser(username = "1")
    void getAnalyticsSummary_WhenNoData_ShouldReturnNotFound() throws Exception {
        when(analyticsService.getAnalyticsSummary(anyLong()))
                .thenReturn(Mono.empty());

        mockMvc.perform(get("/api/dashboard/analytics/summary"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "1")
    void saveReport_ShouldReturnSavedReport() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("period", "monthly");
        
        SaveReportResponse response = SaveReportResponse.builder()
                .reportId(1L)
                .employeeId(1L)
                .reportName("Monthly Report")
                .reportParameters(parameters)
                .createdAt(LocalDateTime.now())
                .build();

        when(analyticsService.saveReport(anyLong(), any(SaveReportRequest.class)))
                .thenReturn(response);

        String requestBody = """
                {
                    "reportName": "Monthly Report",
                    "reportParameters": {
                        "period": "monthly"
                    }
                }
                """;

        mockMvc.perform(post("/api/dashboard/analytics/save-report")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId").value(1))
                .andExpect(jsonPath("$.employeeId").value(1))
                .andExpect(jsonPath("$.reportName").value("Monthly Report"))
                .andExpect(jsonPath("$.reportParameters.period").value("monthly"));
    }

    @Test
    @WithMockUser(username = "1")
    void getSavedReports_ShouldReturnListOfReports() throws Exception {
        Map<String, Object> params1 = new HashMap<>();
        params1.put("period", "weekly");
        
        Map<String, Object> params2 = new HashMap<>();
        params2.put("period", "monthly");
        
        SaveReportResponse report1 = SaveReportResponse.builder()
                .reportId(1L)
                .employeeId(1L)
                .reportName("Report 1")
                .reportParameters(params1)
                .createdAt(LocalDateTime.now())
                .build();

        SaveReportResponse report2 = SaveReportResponse.builder()
                .reportId(2L)
                .employeeId(1L)
                .reportName("Report 2")
                .reportParameters(params2)
                .createdAt(LocalDateTime.now())
                .build();

        when(analyticsService.getSavedReports(anyLong()))
                .thenReturn(Arrays.asList(report1, report2));

        mockMvc.perform(get("/api/dashboard/analytics/saved-reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reportName").value("Report 1"))
                .andExpect(jsonPath("$[1].reportName").value("Report 2"))
                .andExpect(jsonPath("$[0].reportParameters.period").value("weekly"))
                .andExpect(jsonPath("$[1].reportParameters.period").value("monthly"));
    }

    @Test
    @WithMockUser(username = "1")
    void getSavedReports_WhenNoReports_ShouldReturnEmptyList() throws Exception {
        when(analyticsService.getSavedReports(anyLong()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/dashboard/analytics/saved-reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
