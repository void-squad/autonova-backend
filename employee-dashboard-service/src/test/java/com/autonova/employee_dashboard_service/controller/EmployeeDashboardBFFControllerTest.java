package com.autonova.employee_dashboard_service.controller;

import com.autonova.employee_dashboard_service.dto.EmployeeDashboardResponse;
import com.autonova.employee_dashboard_service.service.EmployeeDashboardBFFService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller Integration Tests - Simple tests for endpoints
 * Note: Full security integration would require running auth-service or mocking JWT tokens
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("EmployeeDashboardBFFController Basic Tests")
class EmployeeDashboardBFFControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeDashboardBFFService bffService;

    @Test
    @DisplayName("Should return 200 OK for health check endpoint with authenticated user")
    @WithMockUser(roles = {"EMPLOYEE"})
    void shouldReturnHealthCheck() throws Exception {
        mockMvc.perform(get("/api/employee-dashboard/health").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Employee Dashboard BFF is running"));
    }

    @Test
    @DisplayName("Should return dashboard data for authenticated employee with role")
    @WithMockUser(username = "employee@autonova.com", roles = {"EMPLOYEE"})
    void shouldReturnDashboardDataForAuthenticatedEmployee() throws Exception {
        EmployeeDashboardResponse mockResponse = sampleDashboardResponse();

        when(bffService.getEmployeeDashboard(anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(reactor.core.publisher.Mono.just(mockResponse));

        // When/Then
        mockMvc.perform(get("/api/employee-dashboard")
                        .requestAttr("userId", 123L)
                        .requestAttr("userRole", "EMPLOYEE")
                        .header("Authorization", "Bearer test-token")
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should authorize real JWT with role prefix and return dashboard data")
    void shouldAuthorizeRealJwtWithRolePrefix() throws Exception {
        EmployeeDashboardResponse mockResponse = sampleDashboardResponse();

        when(bffService.getEmployeeDashboard(anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(reactor.core.publisher.Mono.just(mockResponse));

        String jwt = generateTestToken(456L, "employee@autonova.com", "ROLE_EMPLOYEE");

        mockMvc.perform(get("/api/employee-dashboard")
                        .header("Authorization", "Bearer " + jwt)
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    private EmployeeDashboardResponse sampleDashboardResponse() {
        return EmployeeDashboardResponse.builder()
                .employeeInfo(EmployeeDashboardResponse.EmployeeInfo.builder()
                        .userId(123L)
                        .name("Test Employee")
                        .email("employee@autonova.com")
                        .role("EMPLOYEE")
                        .department("Service")
                        .build())
                .stats(EmployeeDashboardResponse.DashboardStats.builder()
                        .totalActiveProjects(5)
                        .pendingAppointments(3)
                        .completedTasksThisWeek(10)
                        .totalRevenueThisMonth(50000.0)
                        .totalCustomers(25)
                        .build())
                .recentActivities(new ArrayList<>())
                .upcomingTasks(new ArrayList<>())
                .activeProjects(new ArrayList<>())
                .build();
    }

        private String generateTestToken(Long userId, String email, String roleClaim) {
        Instant now = Instant.now();
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", roleClaim);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret)), SignatureAlgorithm.HS256)
                .compact();
    }

    @Value("${jwt.secret}")
    private String jwtSecret;
}
