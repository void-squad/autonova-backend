package com.automobileservice.time_logging_service.service;

import com.automobileservice.time_logging_service.client.AuthServiceClient;
import com.automobileservice.time_logging_service.client.ProjectServiceClient;
import com.automobileservice.time_logging_service.dto.request.TimeLogRequest;
import com.automobileservice.time_logging_service.dto.response.TimeLogResponse;
import com.automobileservice.time_logging_service.entity.TimeLog;
import com.automobileservice.time_logging_service.exception.ResourceNotFoundException;
import com.automobileservice.time_logging_service.repository.TimeLogRepository;
import com.automobileservice.time_logging_service.service.impl.TimeLogServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeLogServiceImplTest {

    @Mock
    private TimeLogRepository timeLogRepository;

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private ProjectServiceClient projectServiceClient;

    @InjectMocks
    private TimeLogServiceImpl timeLogService;

    private UUID projectId;
    private UUID taskId;
    private Long employeeId;
    private TimeLogRequest validRequest;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        taskId = UUID.randomUUID();
        employeeId = 1L;

        validRequest = new TimeLogRequest();
        validRequest.setEmployeeId(employeeId);
        validRequest.setProjectId(projectId);
        validRequest.setTaskId(taskId);
        validRequest.setHours(new BigDecimal("8.5"));
        validRequest.setNote("Working on feature X");
    }

    @Test
    void createTimeLog_withValidRequest_createsTimeLog() {
        // Given
        when(authServiceClient.getUserById(employeeId)).thenReturn(null); // Mock response
        when(projectServiceClient.getProjectById(projectId)).thenReturn(null);
        when(projectServiceClient.getTaskById(projectId, taskId)).thenReturn(null);
        when(timeLogRepository.save(any(TimeLog.class)))
                .thenAnswer(invocation -> {
                    TimeLog timeLog = invocation.getArgument(0);
                    timeLog.setId(UUID.randomUUID());
                    timeLog.setLoggedAt(LocalDateTime.now());
                    return timeLog;
                });

        // When
        TimeLogResponse result = timeLogService.createTimeLog(validRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmployeeId()).isEqualTo(employeeId);
        assertThat(result.getProjectId()).isEqualTo(projectId);
        assertThat(result.getTaskId()).isEqualTo(taskId);
        assertThat(result.getHours()).isEqualTo(new BigDecimal("8.5"));
        assertThat(result.getApprovalStatus()).isEqualTo("PENDING");

        verify(timeLogRepository).save(any(TimeLog.class));
        verify(authServiceClient).getUserById(employeeId);
        verify(projectServiceClient).getProjectById(projectId);
        verify(projectServiceClient).getTaskById(projectId, taskId);
    }

    @Test
    void createTimeLog_withInvalidEmployee_throwsResourceNotFoundException() {
        // Given
        when(authServiceClient.getUserById(employeeId))
                .thenThrow(new RuntimeException("User not found"));

        // When/Then
        assertThatThrownBy(() -> timeLogService.createTimeLog(validRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("One or more referenced resources not found");

        verify(timeLogRepository, never()).save(any());
    }

    @Test
    void createTimeLog_withInvalidProject_throwsResourceNotFoundException() {
        // Given
        when(authServiceClient.getUserById(employeeId)).thenReturn(null);
        when(projectServiceClient.getProjectById(projectId))
                .thenThrow(new RuntimeException("Project not found"));

        // When/Then
        assertThatThrownBy(() -> timeLogService.createTimeLog(validRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("One or more referenced resources not found");

        verify(timeLogRepository, never()).save(any());
    }

    @Test
    void updateTimeLog_existingTimeLog_updatesSuccessfully() {
        // Given
        UUID timeLogId = UUID.randomUUID();
        TimeLog existingTimeLog = createTestTimeLog(timeLogId);

        when(timeLogRepository.findById(timeLogId)).thenReturn(Optional.of(existingTimeLog));
        when(authServiceClient.getUserById(employeeId)).thenReturn(null);
        when(projectServiceClient.getProjectById(projectId)).thenReturn(null);
        when(projectServiceClient.getTaskById(projectId, taskId)).thenReturn(null);
        when(timeLogRepository.save(any(TimeLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        TimeLogResponse result = timeLogService.updateTimeLog(timeLogId, validRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getHours()).isEqualTo(new BigDecimal("8.5"));
        verify(timeLogRepository).save(any(TimeLog.class));
    }

    @Test
    void updateTimeLog_nonExistentTimeLog_throwsResourceNotFoundException() {
        // Given
        UUID timeLogId = UUID.randomUUID();
        when(timeLogRepository.findById(timeLogId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> timeLogService.updateTimeLog(timeLogId, validRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("TimeLog");

        verify(timeLogRepository, never()).save(any());
    }

    @Test
    void deleteTimeLog_existingTimeLog_deletesSuccessfully() {
        // Given
        UUID timeLogId = UUID.randomUUID();
        TimeLog existingTimeLog = createTestTimeLog(timeLogId);

        when(timeLogRepository.findById(timeLogId)).thenReturn(Optional.of(existingTimeLog));
        doNothing().when(timeLogRepository).delete(existingTimeLog);

        // When
        timeLogService.deleteTimeLog(timeLogId);

        // Then
        verify(timeLogRepository).delete(existingTimeLog);
    }

    @Test
    void deleteTimeLog_nonExistentTimeLog_throwsResourceNotFoundException() {
        // Given
        UUID timeLogId = UUID.randomUUID();
        when(timeLogRepository.findById(timeLogId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> timeLogService.deleteTimeLog(timeLogId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(timeLogRepository, never()).delete(any());
    }

    @Test
    void getTimeLogById_existingTimeLog_returnsTimeLog() {
        // Given
        UUID timeLogId = UUID.randomUUID();
        TimeLog timeLog = createTestTimeLog(timeLogId);

        when(timeLogRepository.findById(timeLogId)).thenReturn(Optional.of(timeLog));

        // When
        TimeLogResponse result = timeLogService.getTimeLogById(timeLogId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(timeLogId);
        assertThat(result.getEmployeeId()).isEqualTo(employeeId);
    }

    @Test
    void getTimeLogById_nonExistentTimeLog_throwsResourceNotFoundException() {
        // Given
        UUID timeLogId = UUID.randomUUID();
        when(timeLogRepository.findById(timeLogId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> timeLogService.getTimeLogById(timeLogId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getTimeLogsByEmployee_returnsEmployeeTimeLogs() {
        // Given
        List<TimeLog> timeLogs = Arrays.asList(
                createTestTimeLog(UUID.randomUUID()),
                createTestTimeLog(UUID.randomUUID())
        );

        when(timeLogRepository.findByEmployeeIdOrderByLoggedAtDesc(employeeId))
                .thenReturn(timeLogs);

        // When
        List<TimeLogResponse> result = timeLogService.getTimeLogsByEmployee(employeeId);

        // Then
        assertThat(result).hasSize(2);
        verify(timeLogRepository).findByEmployeeIdOrderByLoggedAtDesc(employeeId);
    }

    @Test
    void getTimeLogsByProject_returnsProjectTimeLogs() {
        // Given
        List<TimeLog> timeLogs = Arrays.asList(
                createTestTimeLog(UUID.randomUUID()),
                createTestTimeLog(UUID.randomUUID()),
                createTestTimeLog(UUID.randomUUID())
        );

        when(timeLogRepository.findByProjectIdOrderByLoggedAtDesc(projectId))
                .thenReturn(timeLogs);

        // When
        List<TimeLogResponse> result = timeLogService.getTimeLogsByProject(projectId);

        // Then
        assertThat(result).hasSize(3);
        verify(timeLogRepository).findByProjectIdOrderByLoggedAtDesc(projectId);
    }

    @Test
    void getTimeLogsByTask_returnsTaskTimeLogs() {
        // Given
        List<TimeLog> timeLogs = Collections.singletonList(createTestTimeLog(UUID.randomUUID()));

        when(timeLogRepository.findByTaskIdOrderByLoggedAtDesc(taskId))
                .thenReturn(timeLogs);

        // When
        List<TimeLogResponse> result = timeLogService.getTimeLogsByTask(taskId);

        // Then
        assertThat(result).hasSize(1);
        verify(timeLogRepository).findByTaskIdOrderByLoggedAtDesc(taskId);
    }

    @Test
    void getTotalHoursByEmployee_returnsTotalHours() {
        // Given
        BigDecimal expectedHours = new BigDecimal("40.5");
        when(timeLogRepository.getTotalApprovedHoursByEmployee(employeeId))
                .thenReturn(expectedHours);

        // When
        BigDecimal result = timeLogService.getTotalHoursByEmployee(employeeId);

        // Then
        assertThat(result).isEqualTo(expectedHours);
        verify(timeLogRepository).getTotalApprovedHoursByEmployee(employeeId);
    }

    @Test
    void getAllTimeLogs_returnsAllTimeLogs() {
        // Given
        List<TimeLog> timeLogs = Arrays.asList(
                createTestTimeLog(UUID.randomUUID()),
                createTestTimeLog(UUID.randomUUID()),
                createTestTimeLog(UUID.randomUUID()),
                createTestTimeLog(UUID.randomUUID())
        );

        when(timeLogRepository.findAllByOrderByLoggedAtDesc()).thenReturn(timeLogs);

        // When
        List<TimeLogResponse> result = timeLogService.getAllTimeLogs();

        // Then
        assertThat(result).hasSize(4);
        verify(timeLogRepository).findAllByOrderByLoggedAtDesc();
    }

    @Test
    void getPendingTimeLogs_returnsPendingTimeLogs() {
        // Given
        List<TimeLog> pendingTimeLogs = Arrays.asList(
                createTestTimeLog(UUID.randomUUID()),
                createTestTimeLog(UUID.randomUUID())
        );

        when(timeLogRepository.findByApprovalStatusOrderByLoggedAtDesc("PENDING"))
                .thenReturn(pendingTimeLogs);

        // When
        List<TimeLogResponse> result = timeLogService.getPendingTimeLogs();

        // Then
        assertThat(result).hasSize(2);
        verify(timeLogRepository).findByApprovalStatusOrderByLoggedAtDesc("PENDING");
    }

    @Test
    void approveTimeLog_existingTimeLog_approvesSuccessfully() {
        // Given
        UUID timeLogId = UUID.randomUUID();
        Long approvedBy = 2L;
        TimeLog timeLog = createTestTimeLog(timeLogId);
        timeLog.setApprovalStatus("PENDING");

        when(timeLogRepository.findById(timeLogId)).thenReturn(Optional.of(timeLog));
        when(timeLogRepository.save(any(TimeLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        TimeLogResponse result = timeLogService.approveTimeLog(timeLogId, approvedBy);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getApprovalStatus()).isEqualTo("APPROVED");
        verify(timeLogRepository).save(any(TimeLog.class));
    }

    @Test
    void rejectTimeLog_existingTimeLog_rejectsSuccessfully() {
        // Given
        UUID timeLogId = UUID.randomUUID();
        Long rejectedBy = 2L;
        String reason = "Incorrect hours logged";
        TimeLog timeLog = createTestTimeLog(timeLogId);
        timeLog.setApprovalStatus("PENDING");

        when(timeLogRepository.findById(timeLogId)).thenReturn(Optional.of(timeLog));
        when(timeLogRepository.save(any(TimeLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        TimeLogResponse result = timeLogService.rejectTimeLog(timeLogId, rejectedBy, reason);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getApprovalStatus()).isEqualTo("REJECTED");
        verify(timeLogRepository).save(any(TimeLog.class));
    }

    @Test
    void getTimeLogsByEmployeeAndProject_returnsFilteredTimeLogs() {
        // Given
        List<TimeLog> timeLogs = Arrays.asList(
                createTestTimeLog(UUID.randomUUID()),
                createTestTimeLog(UUID.randomUUID())
        );

        when(timeLogRepository.findByEmployeeIdAndProjectIdOrderByLoggedAtDesc(employeeId, projectId))
                .thenReturn(timeLogs);

        // When
        List<TimeLogResponse> result = timeLogService.getTimeLogsByEmployeeAndProject(employeeId, projectId);

        // Then
        assertThat(result).hasSize(2);
        verify(timeLogRepository).findByEmployeeIdAndProjectIdOrderByLoggedAtDesc(employeeId, projectId);
    }

    private TimeLog createTestTimeLog(UUID id) {
        TimeLog timeLog = new TimeLog();
        timeLog.setId(id);
        timeLog.setEmployeeId(employeeId);
        timeLog.setProjectId(projectId);
        timeLog.setTaskId(taskId);
        timeLog.setHours(new BigDecimal("8.0"));
        timeLog.setNote("Test time log");
        timeLog.setApprovalStatus("PENDING");
        timeLog.setLoggedAt(LocalDateTime.now());
        return timeLog;
    }
}
