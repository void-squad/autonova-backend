package com.automobileservice.time_logging_service.service.impl;

import com.automobileservice.time_logging_service.client.AuthServiceClient;
import com.automobileservice.time_logging_service.client.ProjectServiceClient;
import com.automobileservice.time_logging_service.dto.request.TimeLogRequest;
import com.automobileservice.time_logging_service.dto.response.TimeLogResponse;
import com.automobileservice.time_logging_service.entity.TimeLog;
import com.automobileservice.time_logging_service.exception.ResourceNotFoundException;
import com.automobileservice.time_logging_service.repository.TimeLogRepository;
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
    private final AuthServiceClient authServiceClient;
    private final ProjectServiceClient projectServiceClient;
    
    @Override
    public TimeLogResponse createTimeLog(TimeLogRequest request) {
        log.info("Creating time log for employee: {}, project: {}, task: {}", 
            request.getEmployeeId(), request.getProjectId(), request.getTaskId());
        
        // Basic validation - verify external resources exist by calling Feign clients
        // This will throw FeignException if resources don't exist
        try {
            authServiceClient.getUserById(request.getEmployeeId());
            projectServiceClient.getProjectById(request.getProjectId());
            projectServiceClient.getTaskById(request.getProjectId(), request.getTaskId());
        } catch (Exception e) {
            log.error("Validation failed: {}", e.getMessage());
            throw new ResourceNotFoundException("One or more referenced resources not found: " + e.getMessage());
        }
        
        TimeLog timeLog = new TimeLog();
        timeLog.setEmployeeId(request.getEmployeeId());
        timeLog.setProjectId(request.getProjectId());
        timeLog.setTaskId(request.getTaskId());
        timeLog.setHours(request.getHours());
        timeLog.setNote(request.getNote());
        timeLog.setApprovalStatus("PENDING");
        timeLog.setLoggedAt(LocalDateTime.now());
        
        TimeLog savedTimeLog = timeLogRepository.save(timeLog);
        
        log.info("Time log created successfully: {}", savedTimeLog.getId());
        return mapToResponse(savedTimeLog);
    }
    
    @Override
    public TimeLogResponse updateTimeLog(UUID id, TimeLogRequest request) {
        log.info("Updating time log: {}", id);
        
        TimeLog timeLog = timeLogRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("TimeLog", "id", id.toString()));
        
        // Basic validation
        try {
            authServiceClient.getUserById(request.getEmployeeId());
            projectServiceClient.getProjectById(request.getProjectId());
            projectServiceClient.getTaskById(request.getProjectId(), request.getTaskId());
        } catch (Exception e) {
            log.error("Validation failed: {}", e.getMessage());
            throw new ResourceNotFoundException("One or more referenced resources not found: " + e.getMessage());
        }
        
        timeLog.setEmployeeId(request.getEmployeeId());
        timeLog.setProjectId(request.getProjectId());
        timeLog.setTaskId(request.getTaskId());
        timeLog.setHours(request.getHours());
        timeLog.setNote(request.getNote());
        
        TimeLog updatedTimeLog = timeLogRepository.save(timeLog);
        
        log.info("Time log updated successfully: {}", id);
        return mapToResponse(updatedTimeLog);
    }
    
    @Override
    public void deleteTimeLog(UUID id) {
        log.info("Deleting time log: {}", id);
        
        TimeLog timeLog = timeLogRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("TimeLog", "id", id.toString()));
        
        timeLogRepository.delete(timeLog);
        
        log.info("Time log deleted successfully: {}", id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public TimeLogResponse getTimeLogById(UUID id) {
        TimeLog timeLog = timeLogRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("TimeLog", "id", id.toString()));
        return mapToResponse(timeLog);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TimeLogResponse> getTimeLogsByEmployee(String employeeId) {
        List<TimeLog> timeLogs = timeLogRepository.findByEmployeeIdOrderByLoggedAtDesc(employeeId);
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
        return timeLogRepository.getTotalApprovedHoursByEmployee(employeeId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TimeLogResponse> getAllTimeLogs() {
        List<TimeLog> timeLogs = timeLogRepository.findAllByOrderByLoggedAtDesc();
        return timeLogs.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TimeLogResponse> getPendingTimeLogs() {
        List<TimeLog> timeLogs = timeLogRepository.findByApprovalStatusOrderByLoggedAtDesc("PENDING");
        return timeLogs.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    @Override
    public TimeLogResponse approveTimeLog(UUID id, String approvedBy) {
        log.info("Approving time log: {} by user: {}", id, approvedBy);
        
        TimeLog timeLog = timeLogRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("TimeLog", "id", id.toString()));
        
        timeLog.setApprovalStatus("APPROVED");
        timeLog.setApprovedBy(approvedBy);
        timeLog.setApprovedAt(LocalDateTime.now());
        timeLog.setRejectionReason(null);
        
        TimeLog updatedTimeLog = timeLogRepository.save(timeLog);
        
        log.info("Time log approved successfully: {}", id);
        return mapToResponse(updatedTimeLog);
    }
    
    @Override
    public TimeLogResponse rejectTimeLog(UUID id, String rejectedBy, String reason) {
        log.info("Rejecting time log: {} by user: {} with reason: {}", id, rejectedBy, reason);
        
        TimeLog timeLog = timeLogRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("TimeLog", "id", id.toString()));
        
        timeLog.setApprovalStatus("REJECTED");
        timeLog.setRejectionReason(reason);
        timeLog.setApprovedBy(rejectedBy);
        timeLog.setApprovedAt(LocalDateTime.now());
        
        TimeLog updatedTimeLog = timeLogRepository.save(timeLog);
        
        log.info("Time log rejected successfully: {}", id);
        return mapToResponse(updatedTimeLog);
    }
    
    private TimeLogResponse mapToResponse(TimeLog timeLog) {
        TimeLogResponse response = new TimeLogResponse();
        response.setId(timeLog.getId());
        response.setEmployeeId(timeLog.getEmployeeId());
        response.setProjectId(timeLog.getProjectId());
        response.setTaskId(timeLog.getTaskId());
        response.setHours(timeLog.getHours());
        response.setNote(timeLog.getNote());
        response.setApprovalStatus(timeLog.getApprovalStatus());
        response.setRejectionReason(timeLog.getRejectionReason());
        response.setApprovedBy(timeLog.getApprovedBy());
        response.setApprovedAt(timeLog.getApprovedAt());
        response.setLoggedAt(timeLog.getLoggedAt());
        return response;
    }
}
