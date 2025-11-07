package com.autonova.employee_dashboard_service.controller;

import com.autonova.employee_dashboard_service.dto.OperationalViewResponse;
import com.autonova.employee_dashboard_service.service.OperationalDashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OperationalDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OperationalDashboardService operationalDashboardService;

    @Test
    @WithMockUser(username = "1")
    void getOperationalView_ShouldReturnOperationalData() throws Exception {
        OperationalViewResponse.ActiveTimerData activeTimer = OperationalViewResponse.ActiveTimerData.builder()
                .timerId(1L)
                .employeeId(1L)
                .jobId("JOB-123")
                .status("RUNNING")
                .startTime("2025-11-06T10:00:00")
                .elapsedSeconds(3600L)
                .build();

        OperationalViewResponse.AppointmentData appointment = OperationalViewResponse.AppointmentData.builder()
                .appointmentId(1L)
                .customerName("John Doe")
                .appointmentTime("2025-11-06T14:00:00")
                .serviceType("Oil Change")
                .status("CONFIRMED")
                .build();

        OperationalViewResponse.WorkQueueItem workItem = OperationalViewResponse.WorkQueueItem.builder()
                .jobId(1L)
                .jobType("Repair")
                .priority("HIGH")
                .status("PENDING")
                .assignedTo("Employee 1")
                .deadline("2025-11-07T17:00:00")
                .build();

        OperationalViewResponse response = OperationalViewResponse.builder()
                .activeTimer(activeTimer)
                .todaysAppointments(Arrays.asList(appointment))
                .workQueue(Arrays.asList(workItem))
                .build();

        when(operationalDashboardService.getOperationalView(anyLong()))
                .thenReturn(Mono.just(response));

        mockMvc.perform(get("/api/dashboard/operational"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeTimer.timerId").value(1))
                .andExpect(jsonPath("$.activeTimer.jobId").value("JOB-123"))
                .andExpect(jsonPath("$.activeTimer.status").value("RUNNING"))
                .andExpect(jsonPath("$.todaysAppointments[0].customerName").value("John Doe"))
                .andExpect(jsonPath("$.todaysAppointments[0].serviceType").value("Oil Change"))
                .andExpect(jsonPath("$.workQueue[0].jobType").value("Repair"))
                .andExpect(jsonPath("$.workQueue[0].priority").value("HIGH"));
    }

    @Test
    @WithMockUser(username = "1")
    void getOperationalView_WhenNoActiveTimer_ShouldReturnEmptyTimer() throws Exception {
        OperationalViewResponse response = OperationalViewResponse.builder()
                .activeTimer(OperationalViewResponse.ActiveTimerData.builder().build())
                .todaysAppointments(Collections.emptyList())
                .workQueue(Collections.emptyList())
                .build();

        when(operationalDashboardService.getOperationalView(anyLong()))
                .thenReturn(Mono.just(response));

        mockMvc.perform(get("/api/dashboard/operational"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todaysAppointments").isArray())
                .andExpect(jsonPath("$.todaysAppointments").isEmpty())
                .andExpect(jsonPath("$.workQueue").isArray())
                .andExpect(jsonPath("$.workQueue").isEmpty());
    }

    @Test
    @WithMockUser(username = "1")
    void getOperationalView_WhenServiceFails_ShouldReturnNotFound() throws Exception {
        when(operationalDashboardService.getOperationalView(anyLong()))
                .thenReturn(Mono.empty());

        mockMvc.perform(get("/api/dashboard/operational"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "1")
    void getOperationalView_WithMultipleAppointments_ShouldReturnAll() throws Exception {
        OperationalViewResponse.AppointmentData appointment1 = OperationalViewResponse.AppointmentData.builder()
                .appointmentId(1L)
                .customerName("John Doe")
                .appointmentTime("2025-11-06T10:00:00")
                .serviceType("Oil Change")
                .status("CONFIRMED")
                .build();

        OperationalViewResponse.AppointmentData appointment2 = OperationalViewResponse.AppointmentData.builder()
                .appointmentId(2L)
                .customerName("Jane Smith")
                .appointmentTime("2025-11-06T14:00:00")
                .serviceType("Brake Service")
                .status("PENDING")
                .build();

        OperationalViewResponse response = OperationalViewResponse.builder()
                .activeTimer(OperationalViewResponse.ActiveTimerData.builder().build())
                .todaysAppointments(Arrays.asList(appointment1, appointment2))
                .workQueue(Collections.emptyList())
                .build();

        when(operationalDashboardService.getOperationalView(anyLong()))
                .thenReturn(Mono.just(response));

        mockMvc.perform(get("/api/dashboard/operational"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todaysAppointments").isArray())
                .andExpect(jsonPath("$.todaysAppointments[0].customerName").value("John Doe"))
                .andExpect(jsonPath("$.todaysAppointments[1].customerName").value("Jane Smith"))
                .andExpect(jsonPath("$.todaysAppointments[1].serviceType").value("Brake Service"));
    }
}
