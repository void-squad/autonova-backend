package com.automobileservice.time_logging_service.controller;

import com.automobileservice.time_logging_service.dto.request.TimeLogRequest;
import com.automobileservice.time_logging_service.dto.response.*;
import com.automobileservice.time_logging_service.service.TimeLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeLogControllerTest {

    @Mock
    private TimeLogService timeLogService;

    @InjectMocks
    private TimeLogController controller;

    private TimeLogRequest timeLogRequest;
    private TimeLogResponse timeLogResponse;
    private UUID timeLogId;

    @BeforeEach
    void setUp() {
        timeLogId = UUID.randomUUID();
        
        timeLogRequest = new TimeLogRequest();
        timeLogRequest.setEmployeeId(1L);
        timeLogRequest.setProjectId(UUID.randomUUID());
        timeLogRequest.setTaskId(UUID.randomUUID());
        timeLogRequest.setHours(BigDecimal.valueOf(8.0));
        timeLogRequest.setNote("Test work");

        timeLogResponse = TimeLogResponse.builder()
                .id(timeLogId)
                .employeeId(1L)
                .hours(BigDecimal.valueOf(8.0))
                .build();
    }

    @Test
    void createTimeLog_withValidRequest_returnsCreated() {
        when(timeLogService.createTimeLog(any(TimeLogRequest.class))).thenReturn(timeLogResponse);

        ResponseEntity<TimeLogResponse> response = controller.createTimeLog(timeLogRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(timeLogResponse);
        verify(timeLogService).createTimeLog(timeLogRequest);
    }

    @Test
    void updateTimeLog_withValidRequest_returnsOk() {
        when(timeLogService.updateTimeLog(eq(timeLogId), any(TimeLogRequest.class))).thenReturn(timeLogResponse);

        ResponseEntity<TimeLogResponse> response = controller.updateTimeLog(timeLogId, timeLogRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(timeLogResponse);
        verify(timeLogService).updateTimeLog(timeLogId, timeLogRequest);
    }

    @Test
    void deleteTimeLog_withValidId_returnsNoContent() {
        doNothing().when(timeLogService).deleteTimeLog(timeLogId);

        ResponseEntity<Void> response = controller.deleteTimeLog(timeLogId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(timeLogService).deleteTimeLog(timeLogId);
    }

    @Test
    void getTimeLogById_withValidId_returnsTimeLog() {
        when(timeLogService.getTimeLogById(timeLogId)).thenReturn(timeLogResponse);

        ResponseEntity<TimeLogResponse> response = controller.getTimeLogById(timeLogId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(timeLogResponse);
        verify(timeLogService).getTimeLogById(timeLogId);
    }

    @Test
    void getTimeLogsByEmployee_returnsListOfTimeLogs() {
        Long employeeId = 1L;
        List<TimeLogResponse> timeLogs = Arrays.asList(timeLogResponse);
        when(timeLogService.getTimeLogsByEmployee(employeeId)).thenReturn(timeLogs);

        ResponseEntity<List<TimeLogResponse>> response = controller.getTimeLogsByEmployee(employeeId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(timeLogService).getTimeLogsByEmployee(employeeId);
    }

    @Test
    void getTimeLogsByProject_returnsListOfTimeLogs() {
        UUID projectId = UUID.randomUUID();
        List<TimeLogResponse> timeLogs = Arrays.asList(timeLogResponse);
        when(timeLogService.getTimeLogsByProject(projectId)).thenReturn(timeLogs);

        ResponseEntity<List<TimeLogResponse>> response = controller.getTimeLogsByProject(projectId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(timeLogService).getTimeLogsByProject(projectId);
    }

    @Test
    void getTimeLogsByTask_returnsListOfTimeLogs() {
        UUID taskId = UUID.randomUUID();
        List<TimeLogResponse> timeLogs = Arrays.asList(timeLogResponse);
        when(timeLogService.getTimeLogsByTask(taskId)).thenReturn(timeLogs);

        ResponseEntity<List<TimeLogResponse>> response = controller.getTimeLogsByTask(taskId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(timeLogService).getTimeLogsByTask(taskId);
    }

    @Test
    void getTotalHoursByEmployee_returnsTotalHours() {
        Long employeeId = 1L;
        BigDecimal totalHours = BigDecimal.valueOf(40.0);
        when(timeLogService.getTotalHoursByEmployee(employeeId)).thenReturn(totalHours);

        ResponseEntity<BigDecimal> response = controller.getTotalHoursByEmployee(employeeId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(totalHours);
        verify(timeLogService).getTotalHoursByEmployee(employeeId);
    }

    @Test
    void getPendingTimeLogs_returnsListOfPendingTimeLogs() {
        List<TimeLogResponse> timeLogs = Arrays.asList(timeLogResponse);
        when(timeLogService.getPendingTimeLogs()).thenReturn(timeLogs);

        ResponseEntity<List<TimeLogResponse>> response = controller.getPendingTimeLogs();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(timeLogService).getPendingTimeLogs();
    }

    @Test
    void getAllTimeLogs_returnsListOfAllTimeLogs() {
        List<TimeLogResponse> timeLogs = Arrays.asList(timeLogResponse);
        when(timeLogService.getAllTimeLogs()).thenReturn(timeLogs);

        ResponseEntity<List<TimeLogResponse>> response = controller.getAllTimeLogs();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(timeLogService).getAllTimeLogs();
    }

    @Test
    void approveTimeLog_withApprovedBy_approvesTimeLog() {
        Map<String, Object> body = new HashMap<>();
        body.put("approvedBy", 2L);
        when(timeLogService.approveTimeLog(timeLogId, 2L)).thenReturn(timeLogResponse);

        ResponseEntity<TimeLogResponse> response = controller.approveTimeLog(timeLogId, body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(timeLogResponse);
        verify(timeLogService).approveTimeLog(timeLogId, 2L);
    }

    @Test
    void approveTimeLog_withoutApprovedBy_usesDefaultUser() {
        when(timeLogService.approveTimeLog(timeLogId, 1L)).thenReturn(timeLogResponse);

        ResponseEntity<TimeLogResponse> response = controller.approveTimeLog(timeLogId, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(timeLogService).approveTimeLog(timeLogId, 1L);
    }

    @Test
    void rejectTimeLog_withReasonAndRejectedBy_rejectsTimeLog() {
        Map<String, Object> body = new HashMap<>();
        body.put("rejectedBy", 2L);
        body.put("reason", "Incorrect hours");
        when(timeLogService.rejectTimeLog(timeLogId, 2L, "Incorrect hours")).thenReturn(timeLogResponse);

        ResponseEntity<TimeLogResponse> response = controller.rejectTimeLog(timeLogId, body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(timeLogResponse);
        verify(timeLogService).rejectTimeLog(timeLogId, 2L, "Incorrect hours");
    }

    @Test
    void rejectTimeLog_withoutReason_usesDefaultReason() {
        Map<String, Object> body = new HashMap<>();
        body.put("rejectedBy", 2L);
        when(timeLogService.rejectTimeLog(timeLogId, 2L, "No reason provided")).thenReturn(timeLogResponse);

        ResponseEntity<TimeLogResponse> response = controller.rejectTimeLog(timeLogId, body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(timeLogService).rejectTimeLog(timeLogId, 2L, "No reason provided");
    }

    @Test
    void getEmployeeSummary_returnsEmployeeSummary() {
        Long employeeId = 1L;
        EmployeeSummaryResponse summary = EmployeeSummaryResponse.builder()
                .employeeId(String.valueOf(employeeId))
                .totalHoursLogged(BigDecimal.valueOf(160))
                .build();
        when(timeLogService.getEmployeeSummary(employeeId)).thenReturn(summary);

        ResponseEntity<EmployeeSummaryResponse> response = controller.getEmployeeSummary(employeeId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(summary);
        verify(timeLogService).getEmployeeSummary(employeeId);
    }

    @Test
    void getWeeklySummary_returnsWeeklySummary() {
        Long employeeId = 1L;
        WeeklySummaryResponse summary = WeeklySummaryResponse.builder().build();
        when(timeLogService.getWeeklySummary(employeeId)).thenReturn(summary);

        ResponseEntity<WeeklySummaryResponse> response = controller.getWeeklySummary(employeeId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(summary);
        verify(timeLogService).getWeeklySummary(employeeId);
    }

    @Test
    void getSmartSuggestions_returnsListOfSuggestions() {
        Long employeeId = 1L;
        SmartSuggestionResponse suggestion = SmartSuggestionResponse.builder()
                .reason("Task completion")
                .urgency("high")
                .build();
        List<SmartSuggestionResponse> suggestions = Arrays.asList(suggestion);
        when(timeLogService.getSmartSuggestions(employeeId)).thenReturn(suggestions);

        ResponseEntity<List<SmartSuggestionResponse>> response = controller.getSmartSuggestions(employeeId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(timeLogService).getSmartSuggestions(employeeId);
    }

    @Test
    void getEfficiencyMetrics_returnsEfficiencyMetrics() {
        Long employeeId = 1L;
        EfficiencyMetricsResponse metrics = EfficiencyMetricsResponse.builder()
                .efficiency(BigDecimal.valueOf(85.5))
                .build();
        when(timeLogService.getEfficiencyMetrics(employeeId)).thenReturn(metrics);

        ResponseEntity<EfficiencyMetricsResponse> response = controller.getEfficiencyMetrics(employeeId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(metrics);
        verify(timeLogService).getEfficiencyMetrics(employeeId);
    }

    @Test
    void getTimeLogsByEmployeeAndProject_returnsListOfTimeLogs() {
        Long employeeId = 1L;
        UUID projectId = UUID.randomUUID();
        List<TimeLogResponse> timeLogs = Arrays.asList(timeLogResponse);
        when(timeLogService.getTimeLogsByEmployeeAndProject(employeeId, projectId)).thenReturn(timeLogs);

        ResponseEntity<List<TimeLogResponse>> response = controller.getTimeLogsByEmployeeAndProject(employeeId, projectId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(timeLogService).getTimeLogsByEmployeeAndProject(employeeId, projectId);
    }
}
