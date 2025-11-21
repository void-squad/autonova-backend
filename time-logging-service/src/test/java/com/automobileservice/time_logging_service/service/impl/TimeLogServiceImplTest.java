package com.automobileservice.time_logging_service.service.impl;

import com.automobileservice.time_logging_service.entity.TimeLog;
import com.automobileservice.time_logging_service.repository.TimeLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class TimeLogServiceImplTest {

    @Mock
    TimeLogRepository timeLogRepository;

    // clients are not used directly by tested methods, provide nulls
    TimeLogServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new TimeLogServiceImpl(timeLogRepository, null, null);
    }

    @Test
    void getEmployeeSummary_calculatesEarningsAndRates() {
        Long empId = 42L;
        when(timeLogRepository.getTotalHoursByEmployee(empId)).thenReturn(new BigDecimal("100.00"));
        when(timeLogRepository.getTotalApprovedHoursByEmployee(empId)).thenReturn(new BigDecimal("40.00"));
        when(timeLogRepository.findByEmployeeIdAndApprovalStatusOrderByLoggedAtDesc(empId, "PENDING"))
            .thenReturn(List.of());

        var resp = service.getEmployeeSummary(empId);

        assertThat(resp.getHourlyRate()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(resp.getTotalEarnings()).isEqualByComparingTo(new BigDecimal("2000.00"));
        assertThat(resp.getTotalHoursLogged()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void getWeeklySummary_groupsByDayAndProject() {
        Long empId = 7L;

        TimeLog t1 = new TimeLog();
        t1.setEmployeeId(empId);
        t1.setProjectId(UUID.randomUUID());
        t1.setTaskId(UUID.randomUUID());
        t1.setHours(new BigDecimal("2.5"));
        t1.setLoggedAt(LocalDateTime.now());

        TimeLog t2 = new TimeLog();
        t2.setEmployeeId(empId);
        t2.setProjectId(UUID.randomUUID());
        t2.setTaskId(UUID.randomUUID());
        t2.setHours(new BigDecimal("3.0"));
        t2.setLoggedAt(LocalDateTime.now().minusDays(2));

        when(timeLogRepository.findByEmployeeIdAndLoggedAtBetween(empId, org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
            .thenReturn(List.of(t1, t2));

        var resp = service.getWeeklySummary(empId);

        assertThat(resp.getDailyHours()).isNotEmpty();
        assertThat(resp.getProjectBreakdown()).isNotEmpty();
    }
}
