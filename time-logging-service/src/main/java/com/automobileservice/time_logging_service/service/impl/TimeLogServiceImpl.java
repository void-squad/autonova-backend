package com.automobileservice.time_logging_service.service.impl;

import com.automobileservice.time_logging_service.dto.request.TimeLogRequest;
import com.automobileservice.time_logging_service.dto.response.EmployeeSummaryResponse;
import com.automobileservice.time_logging_service.dto.response.TimeLogResponse;
import com.automobileservice.time_logging_service.entity.*;
import com.automobileservice.time_logging_service.exception.BusinessRuleException;
import com.automobileservice.time_logging_service.exception.ResourceNotFoundException;
import com.automobileservice.time_logging_service.repository.*;
import com.automobileservice.time_logging_service.service.TimeLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TimeLogServiceImpl implements TimeLogService {
    
    private final TimeLogRepository timeLogRepository;
    private final ProjectRepository projectRepository;
    private final ProjectTaskRepository projectTaskRepository;
    private final EmployeeRepository employeeRepository;
    
    @Override
    public TimeLogResponse createTimeLog(TimeLogRequest request) {
        log.info("Creating time log for employee: {}, project: {}, task: {}", 
            request.getEmployeeId(), request.getProjectId(), request.getTaskId());
        
        // Validate employee exists
        Employee employee = employeeRepository.findById(request.getEmployeeId())
            .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.getEmployeeId()));
        
        // Validate project exists and is active
        Project project = projectRepository.findById(request.getProjectId())
            .orElseThrow(() -> new ResourceNotFoundException("Project", "id", request.getProjectId()));
        
        if ("COMPLETED".equals(project.getStatus()) || "CANCELLED".equals(project.getStatus())) {
            throw new BusinessRuleException("Cannot log time on a completed or cancelled project");
        }
        
        // Validate task exists and belongs to the project
        ProjectTask task = projectTaskRepository.findById(request.getTaskId())
            .orElseThrow(() -> new ResourceNotFoundException("Task", "id", request.getTaskId()));
        
        if (!task.getProject().getId().equals(project.getId())) {
            throw new BusinessRuleException("Task does not belong to the specified project");
        }
        
        // Validate employee is assigned to the task
        if (task.getAssignedEmployee() == null || 
            !task.getAssignedEmployee().getUserId().equals(employee.getUserId())) {
            throw new BusinessRuleException("Employee is not assigned to this task");
        }
        
        // Create time log
        TimeLog timeLog = new TimeLog();
        timeLog.setId(UUID.randomUUID().toString());
        timeLog.setEmployee(employee);
        timeLog.setProject(project);
        timeLog.setTask(task);
        timeLog.setHours(request.getHours());
        timeLog.setNote(request.getNote());
        timeLog.setLoggedAt(LocalDateTime.now());
        
        TimeLog savedTimeLog = timeLogRepository.save(timeLog);
        
        // Update task actual hours
        updateTaskActualHours(task);
        
        log.info("Time log created successfully: {}", savedTimeLog.getId());
        return mapToResponse(savedTimeLog);
    }
    
    @Override
    public TimeLogResponse updateTimeLog(String id, TimeLogRequest request) {
        log.info("Updating time log: {}", id);
        
        TimeLog timeLog = timeLogRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("TimeLog", "id", id));
        
        // Validate new values (same as create)
        Employee employee = employeeRepository.findById(request.getEmployeeId())
            .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.getEmployeeId()));
        
        Project project = projectRepository.findById(request.getProjectId())
            .orElseThrow(() -> new ResourceNotFoundException("Project", "id", request.getProjectId()));
        
        ProjectTask task = projectTaskRepository.findById(request.getTaskId())
            .orElseThrow(() -> new ResourceNotFoundException("Task", "id", request.getTaskId()));
        
        // Update fields
        timeLog.setEmployee(employee);
        timeLog.setProject(project);
        timeLog.setTask(task);
        timeLog.setHours(request.getHours());
        timeLog.setNote(request.getNote());
        
        TimeLog updatedTimeLog = timeLogRepository.save(timeLog);
        
        // Recalculate task actual hours
        updateTaskActualHours(task);
        
        log.info("Time log updated successfully: {}", id);
        return mapToResponse(updatedTimeLog);
    }
    
    @Override
    public void deleteTimeLog(String id) {
        log.info("Deleting time log: {}", id);
        
        TimeLog timeLog = timeLogRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("TimeLog", "id", id));
        
        ProjectTask task = timeLog.getTask();
        
        timeLogRepository.delete(timeLog);
        
        // Recalculate task actual hours after deletion
        updateTaskActualHours(task);
        
        log.info("Time log deleted successfully: {}", id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public TimeLogResponse getTimeLogById(String id) {
        TimeLog timeLog = timeLogRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("TimeLog", "id", id));
        return mapToResponse(timeLog);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TimeLogResponse> getTimeLogsByEmployee(String employeeId) {
        List<TimeLog> timeLogs = timeLogRepository.findByEmployeeUserIdOrderByLoggedAtDesc(employeeId);
        return timeLogs.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TimeLogResponse> getTimeLogsByProject(String projectId) {
        List<TimeLog> timeLogs = timeLogRepository.findByProjectIdOrderByLoggedAtDesc(projectId);
        return timeLogs.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TimeLogResponse> getTimeLogsByTask(String taskId) {
        List<TimeLog> timeLogs = timeLogRepository.findByTaskIdOrderByLoggedAtDesc(taskId);
        return timeLogs.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalHoursByEmployee(String employeeId) {
        return timeLogRepository.getTotalHoursByEmployee(employeeId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public EmployeeSummaryResponse getEmployeeSummary(String employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));
        
        BigDecimal totalHours = timeLogRepository.getTotalHoursByEmployee(employeeId);
        BigDecimal totalEarnings = totalHours.multiply(employee.getHourlyRate());
        
        return EmployeeSummaryResponse.builder()
            .employeeId(employee.getUserId())
            .employeeName(employee.getUser().getFirstName() + " " + employee.getUser().getLastName())
            .department(employee.getDepartment())
            .totalHoursLogged(totalHours)
            .hourlyRate(employee.getHourlyRate())
            .totalEarnings(totalEarnings)
            .build();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TimeLogResponse> getTimeLogsByEmployeeAndProject(String employeeId, String projectId) {
        List<TimeLog> timeLogs = timeLogRepository.findByEmployeeUserIdAndProjectIdOrderByLoggedAtDesc(
            employeeId, projectId);
        return timeLogs.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    // Helper method to update task actual hours
    private void updateTaskActualHours(ProjectTask task) {
        BigDecimal totalHours = timeLogRepository.getTotalHoursByTask(task.getId());
        task.setActualHours(totalHours);
        projectTaskRepository.save(task);
        log.debug("Updated task {} actual hours to {}", task.getId(), totalHours);
    }
    
    // Helper method to map entity to response DTO
    private TimeLogResponse mapToResponse(TimeLog timeLog) {
        return TimeLogResponse.builder()
            .id(timeLog.getId())
            .projectId(timeLog.getProject().getId())
            .projectTitle(timeLog.getProject().getTitle())
            .taskId(timeLog.getTask().getId())
            .taskName(timeLog.getTask().getTaskName())
            .employeeId(timeLog.getEmployee().getUserId())
            .employeeName(timeLog.getEmployee().getUser().getFirstName() + " " + 
                         timeLog.getEmployee().getUser().getLastName())
            .hours(timeLog.getHours())
            .note(timeLog.getNote())
            .loggedAt(timeLog.getLoggedAt())
            .build();
    }
}