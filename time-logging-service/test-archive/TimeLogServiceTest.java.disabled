package com.automobileservice.time_logging_service.service;

import com.automobileservice.time_logging_service.dto.request.TimeLogRequest;
import com.automobileservice.time_logging_service.dto.response.TimeLogResponse;
import com.automobileservice.time_logging_service.entity.*;
import com.automobileservice.time_logging_service.exception.BusinessRuleException;
import com.automobileservice.time_logging_service.exception.ResourceNotFoundException;
import com.automobileservice.time_logging_service.repository.*;
import com.automobileservice.time_logging_service.service.impl.TimeLogServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeLogServiceTest {

    @Mock
    private TimeLogRepository timeLogRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectTaskRepository projectTaskRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private TimeLogServiceImpl timeLogService;

    private Employee employee;
    private Project project;
    private ProjectTask task;
    private TimeLog timeLog;
    private TimeLogRequest request;

    @BeforeEach
    void setUp() {
        // Setup test data
        User user = new User();
        user.setId("emp-001");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");
        user.setRole("EMPLOYEE");

        employee = new Employee();
        employee.setUserId("emp-001");
        employee.setUser(user);
        employee.setEmployeeCode("EMP001");

        project = new Project();
        project.setId("proj-001");
        project.setTitle("Test Project");
        project.setStatus("IN_PROGRESS");

        task = new ProjectTask();
        task.setId("task-001");
        task.setTaskName("Test Task");
        task.setProject(project);
        task.setAssignedEmployee(employee);

        timeLog = new TimeLog();
        timeLog.setId("log-001");
        timeLog.setEmployee(employee);
        timeLog.setProject(project);
        timeLog.setTask(task);
        timeLog.setHours(BigDecimal.valueOf(2.5));
        timeLog.setNote("Test note");
        timeLog.setApprovalStatus("PENDING");
        timeLog.setLoggedAt(LocalDateTime.now());

        request = new TimeLogRequest("emp-001", "proj-001", "task-001", BigDecimal.valueOf(2.5), "Test note");
    }

    @Test
    void createTimeLog_Success() {
        // Arrange
        when(employeeRepository.findById("emp-001")).thenReturn(Optional.of(employee));
        when(projectRepository.findById("proj-001")).thenReturn(Optional.of(project));
        when(projectTaskRepository.findById("task-001")).thenReturn(Optional.of(task));
        when(timeLogRepository.save(any(TimeLog.class))).thenReturn(timeLog);

        // Act
        TimeLogResponse response = timeLogService.createTimeLog(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getEmployeeId()).isEqualTo("emp-001");
        assertThat(response.getProjectId()).isEqualTo("proj-001");
        assertThat(response.getTaskId()).isEqualTo("task-001");
        assertThat(response.getHours()).isEqualTo(BigDecimal.valueOf(2.5));
        assertThat(response.getApprovalStatus()).isEqualTo("PENDING");

        verify(timeLogRepository).save(any(TimeLog.class));
        verify(projectTaskRepository).save(any(ProjectTask.class)); // Task hours update
    }

    @Test
    void createTimeLog_EmployeeNotFound_ThrowsException() {
        // Arrange
        when(employeeRepository.findById("emp-001")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> timeLogService.createTimeLog(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Employee");

        verify(employeeRepository).findById("emp-001");
        verify(timeLogRepository, never()).save(any(TimeLog.class));
    }

    @Test
    void createTimeLog_ProjectCompleted_ThrowsException() {
        // Arrange
        project.setStatus("COMPLETED");
        when(employeeRepository.findById("emp-001")).thenReturn(Optional.of(employee));
        when(projectRepository.findById("proj-001")).thenReturn(Optional.of(project));

        // Act & Assert
        assertThatThrownBy(() -> timeLogService.createTimeLog(request))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Cannot log time on a completed or cancelled project");

        verify(timeLogRepository, never()).save(any(TimeLog.class));
    }

    @Test
    void createTimeLog_EmployeeNotAssignedToTask_ThrowsException() {
        // Arrange
        task.setAssignedEmployee(null); // Employee not assigned
        when(employeeRepository.findById("emp-001")).thenReturn(Optional.of(employee));
        when(projectRepository.findById("proj-001")).thenReturn(Optional.of(project));
        when(projectTaskRepository.findById("task-001")).thenReturn(Optional.of(task));

        // Act & Assert
        assertThatThrownBy(() -> timeLogService.createTimeLog(request))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Employee is not assigned to this task");

        verify(timeLogRepository, never()).save(any(TimeLog.class));
    }

    @Test
    void approveTimeLog_Success() {
        // Arrange
        when(timeLogRepository.findById("log-001")).thenReturn(Optional.of(timeLog));
        when(timeLogRepository.save(any(TimeLog.class))).thenReturn(timeLog);

        // Act
        TimeLogResponse response = timeLogService.approveTimeLog("log-001");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getApprovalStatus()).isEqualTo("APPROVED");

        verify(timeLogRepository).save(timeLog);
        verify(projectTaskRepository).save(any(ProjectTask.class)); // Task hours update
    }

    @Test
    void approveTimeLog_AlreadyApproved_ThrowsException() {
        // Arrange
        timeLog.setApprovalStatus("APPROVED");
        when(timeLogRepository.findById("log-001")).thenReturn(Optional.of(timeLog));

        // Act & Assert
        assertThatThrownBy(() -> timeLogService.approveTimeLog("log-001"))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Time log is already approved");

        verify(timeLogRepository, never()).save(any(TimeLog.class));
    }

    @Test
    void approveTimeLog_RejectedLog_ThrowsException() {
        // Arrange
        timeLog.setApprovalStatus("REJECTED");
        when(timeLogRepository.findById("log-001")).thenReturn(Optional.of(timeLog));

        // Act & Assert
        assertThatThrownBy(() -> timeLogService.approveTimeLog("log-001"))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Cannot approve a rejected time log");

        verify(timeLogRepository, never()).save(any(TimeLog.class));
    }

    @Test
    void rejectTimeLog_Success() {
        // Arrange
        when(timeLogRepository.findById("log-001")).thenReturn(Optional.of(timeLog));
        when(timeLogRepository.save(any(TimeLog.class))).thenReturn(timeLog);

        // Act
        TimeLogResponse response = timeLogService.rejectTimeLog("log-001", "Invalid hours");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getApprovalStatus()).isEqualTo("REJECTED");
        assertThat(response.getNote()).contains("REJECTION REASON: Invalid hours");

        verify(timeLogRepository).save(timeLog);
        // Note: Task hours are not updated for PENDING -> REJECTED transition
        verify(projectTaskRepository, never()).save(any(ProjectTask.class));
    }

    @Test
    void rejectTimeLog_AlreadyRejected_ThrowsException() {
        // Arrange
        timeLog.setApprovalStatus("REJECTED");
        when(timeLogRepository.findById("log-001")).thenReturn(Optional.of(timeLog));

        // Act & Assert
        assertThatThrownBy(() -> timeLogService.rejectTimeLog("log-001", "Test reason"))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Time log is already rejected");

        verify(timeLogRepository, never()).save(any(TimeLog.class));
    }

    @Test
    void rejectTimeLog_ApprovedLog_Success() {
        // Arrange
        timeLog.setApprovalStatus("APPROVED");
        timeLog.setNote("Original note");
        when(timeLogRepository.findById("log-001")).thenReturn(Optional.of(timeLog));
        when(timeLogRepository.save(any(TimeLog.class))).thenReturn(timeLog);

        // Act
        TimeLogResponse response = timeLogService.rejectTimeLog("log-001", "Incorrect task");

        // Assert
        assertThat(response.getApprovalStatus()).isEqualTo("REJECTED");
        assertThat(response.getNote()).contains("Original note");
        assertThat(response.getNote()).contains("REJECTION REASON: Incorrect task");

        verify(timeLogRepository).save(timeLog);
        verify(projectTaskRepository).save(any(ProjectTask.class));
    }

    @Test
    void getPendingTimeLogs_Success() {
        // Arrange
        List<TimeLog> pendingLogs = List.of(timeLog);
        when(timeLogRepository.findByApprovalStatusOrderByLoggedAtDesc("PENDING"))
            .thenReturn(pendingLogs);

        // Act
        List<TimeLogResponse> responses = timeLogService.getPendingTimeLogs();

        // Assert
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getApprovalStatus()).isEqualTo("PENDING");

        verify(timeLogRepository).findByApprovalStatusOrderByLoggedAtDesc("PENDING");
    }

    @Test
    void getAllTimeLogs_Success() {
        // Arrange
        List<TimeLog> allLogs = List.of(timeLog);
        when(timeLogRepository.findAllByOrderByLoggedAtDesc()).thenReturn(allLogs);

        // Act
        List<TimeLogResponse> responses = timeLogService.getAllTimeLogs();

        // Assert
        assertThat(responses).hasSize(1);

        verify(timeLogRepository).findAllByOrderByLoggedAtDesc();
    }

    @Test
    void getTotalHoursByEmployee_CalculatesApprovedOnly() {
        // Arrange
        BigDecimal expectedHours = BigDecimal.valueOf(15.5);
        when(timeLogRepository.getTotalApprovedHoursByEmployee("emp-001"))
            .thenReturn(expectedHours);

        // Act
        BigDecimal result = timeLogService.getTotalHoursByEmployee("emp-001");

        // Assert
        assertThat(result).isEqualTo(expectedHours);

        verify(timeLogRepository).getTotalApprovedHoursByEmployee("emp-001");
    }
}