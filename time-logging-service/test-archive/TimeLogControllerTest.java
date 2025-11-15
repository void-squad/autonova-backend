package com.automobileservice.time_logging_service.controller;

import com.automobileservice.time_logging_service.dto.request.TimeLogRequest;
import com.automobileservice.time_logging_service.dto.response.TimeLogResponse;
import com.automobileservice.time_logging_service.service.TimeLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TimeLogController.class)
class TimeLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TimeLogService timeLogService;

    @Test
    void createTimeLog_Success() throws Exception {
        // Arrange
        TimeLogRequest request = new TimeLogRequest("emp-001", "proj-001", "task-001", BigDecimal.valueOf(2.5), "Test note");

        TimeLogResponse response = TimeLogResponse.builder()
            .id("log-001")
            .employeeId("emp-001")
            .employeeName("John Doe")
            .projectId("proj-001")
            .projectTitle("Test Project")
            .taskId("task-001")
            .taskName("Test Task")
            .hours(BigDecimal.valueOf(2.5))
            .note("Test note")
            .approvalStatus("PENDING")
            .loggedAt(LocalDateTime.now())
            .build();

        when(timeLogService.createTimeLog(any(TimeLogRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/time-logs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("log-001"))
                .andExpect(jsonPath("$.approvalStatus").value("PENDING"))
                .andExpect(jsonPath("$.hours").value(2.5));
    }

    @Test
    void createTimeLog_InvalidRequest_ReturnsBadRequest() throws Exception {
        // Arrange
        TimeLogRequest invalidRequest = new TimeLogRequest("", "proj-001", "task-001", BigDecimal.valueOf(-1), null);

        // Act & Assert
        mockMvc.perform(post("/api/time-logs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllTimeLogs_Success() throws Exception {
        // Arrange
        List<TimeLogResponse> responses = List.of(
            TimeLogResponse.builder()
                .id("log-001")
                .employeeId("emp-001")
                .employeeName("John Doe")
                .projectId("proj-001")
                .projectTitle("Test Project")
                .taskId("task-001")
                .taskName("Test Task")
                .hours(BigDecimal.valueOf(2.5))
                .approvalStatus("PENDING")
                .loggedAt(LocalDateTime.now())
                .build()
        );

        when(timeLogService.getAllTimeLogs()).thenReturn(responses);

        // Act & Assert
        mockMvc.perform(get("/api/time-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("log-001"))
                .andExpect(jsonPath("$[0].approvalStatus").value("PENDING"));
    }

    @Test
    void getPendingTimeLogs_Success() throws Exception {
        // Arrange
        List<TimeLogResponse> responses = List.of(
            TimeLogResponse.builder()
                .id("log-001")
                .employeeId("emp-001")
                .employeeName("John Doe")
                .projectId("proj-001")
                .projectTitle("Test Project")
                .taskId("task-001")
                .taskName("Test Task")
                .hours(BigDecimal.valueOf(2.5))
                .approvalStatus("PENDING")
                .loggedAt(LocalDateTime.now())
                .build()
        );

        when(timeLogService.getPendingTimeLogs()).thenReturn(responses);

        // Act & Assert
        mockMvc.perform(get("/api/time-logs/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("log-001"))
                .andExpect(jsonPath("$[0].approvalStatus").value("PENDING"));
    }

    @Test
    void approveTimeLog_Success() throws Exception {
        // Arrange
        TimeLogResponse response = TimeLogResponse.builder()
            .id("log-001")
            .employeeId("emp-001")
            .employeeName("John Doe")
            .projectId("proj-001")
            .projectTitle("Test Project")
            .taskId("task-001")
            .taskName("Test Task")
            .hours(BigDecimal.valueOf(2.5))
            .approvalStatus("APPROVED")
            .loggedAt(LocalDateTime.now())
            .build();

        when(timeLogService.approveTimeLog("log-001")).thenReturn(response);

        // Act & Assert
        mockMvc.perform(patch("/api/time-logs/log-001/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("log-001"))
                .andExpect(jsonPath("$.approvalStatus").value("APPROVED"));
    }

    @Test
    void rejectTimeLog_Success() throws Exception {
        // Arrange
        TimeLogResponse response = TimeLogResponse.builder()
            .id("log-001")
            .employeeId("emp-001")
            .employeeName("John Doe")
            .projectId("proj-001")
            .projectTitle("Test Project")
            .taskId("task-001")
            .taskName("Test Task")
            .hours(BigDecimal.valueOf(2.5))
            .note("REJECTION REASON: Invalid hours")
            .approvalStatus("REJECTED")
            .loggedAt(LocalDateTime.now())
            .build();

        when(timeLogService.rejectTimeLog(anyString(), anyString())).thenReturn(response);

        // Act & Assert
        mockMvc.perform(patch("/api/time-logs/log-001/reject")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\": \"Invalid hours\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("log-001"))
                .andExpect(jsonPath("$.approvalStatus").value("REJECTED"))
                .andExpect(jsonPath("$.note").value("REJECTION REASON: Invalid hours"));
    }

    @Test
    void rejectTimeLog_MissingReason_ReturnsOkWithDefaultReason() throws Exception {
        // Arrange
        TimeLogResponse response = TimeLogResponse.builder()
                .id("log-001")
                .employeeId("emp-001")
                .employeeName("John Doe")
                .projectId("proj-001")
                .projectTitle("Test Project")
                .taskId("task-001")
                .taskName("Test Task")
                .hours(BigDecimal.valueOf(2.5))
                .approvalStatus("REJECTED")
                .note("REJECTION REASON: No reason provided")
                .loggedAt(LocalDateTime.now())
                .build();

        when(timeLogService.rejectTimeLog("log-001", "No reason provided")).thenReturn(response);

        // Act & Assert
        mockMvc.perform(patch("/api/time-logs/log-001/reject")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalStatus").value("REJECTED"))
                .andExpect(jsonPath("$.note").value("REJECTION REASON: No reason provided"));
    }

    @Test
    void getTimeLogById_Success() throws Exception {
        // Arrange
        TimeLogResponse response = TimeLogResponse.builder()
            .id("log-001")
            .employeeId("emp-001")
            .employeeName("John Doe")
            .projectId("proj-001")
            .projectTitle("Test Project")
            .taskId("task-001")
            .taskName("Test Task")
            .hours(BigDecimal.valueOf(2.5))
            .approvalStatus("PENDING")
            .loggedAt(LocalDateTime.now())
            .build();

        when(timeLogService.getTimeLogById("log-001")).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/time-logs/log-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("log-001"))
                .andExpect(jsonPath("$.approvalStatus").value("PENDING"));
    }

    @Test
    void getTimeLogsByEmployee_Success() throws Exception {
        // Arrange
        List<TimeLogResponse> responses = List.of(
            TimeLogResponse.builder()
                .id("log-001")
                .employeeId("emp-001")
                .employeeName("John Doe")
                .projectId("proj-001")
                .projectTitle("Test Project")
                .taskId("task-001")
                .taskName("Test Task")
                .hours(BigDecimal.valueOf(2.5))
                .approvalStatus("PENDING")
                .loggedAt(LocalDateTime.now())
                .build()
        );

        when(timeLogService.getTimeLogsByEmployee("emp-001")).thenReturn(responses);

        // Act & Assert
        mockMvc.perform(get("/api/time-logs/employee/emp-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("log-001"))
                .andExpect(jsonPath("$[0].employeeId").value("emp-001"));
    }

    @Test
    void getTotalHoursByEmployee_Success() throws Exception {
        // Arrange
        when(timeLogService.getTotalHoursByEmployee("emp-001"))
            .thenReturn(BigDecimal.valueOf(15.5));

        // Act & Assert
        mockMvc.perform(get("/api/time-logs/employee/emp-001/total-hours"))
                .andExpect(status().isOk())
                .andExpect(content().string("15.5"));
    }

    @Test
    void updateTimeLog_Success() throws Exception {
        // Arrange
        TimeLogRequest request = new TimeLogRequest("emp-001", "proj-001", "task-001", BigDecimal.valueOf(3.0), "Updated note");

        TimeLogResponse response = TimeLogResponse.builder()
            .id("log-001")
            .employeeId("emp-001")
            .employeeName("John Doe")
            .projectId("proj-001")
            .projectTitle("Test Project")
            .taskId("task-001")
            .taskName("Test Task")
            .hours(BigDecimal.valueOf(3.0))
            .note("Updated note")
            .approvalStatus("PENDING")
            .loggedAt(LocalDateTime.now())
            .build();

        when(timeLogService.updateTimeLog(anyString(), any(TimeLogRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(put("/api/time-logs/log-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("log-001"))
                .andExpect(jsonPath("$.hours").value(3.0));
    }

    @Test
    void deleteTimeLog_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/time-logs/log-001"))
                .andExpect(status().isNoContent());
    }
}