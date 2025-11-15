package com.automobileservice.time_logging_service.service.impl;

import com.automobileservice.time_logging_service.client.AuthServiceClient;
import com.automobileservice.time_logging_service.client.ProjectServiceClient;
import com.automobileservice.time_logging_service.dto.request.TimeLogRequest;
import com.automobileservice.time_logging_service.dto.response.*;
import com.automobileservice.time_logging_service.entity.TimeLog;
import com.automobileservice.time_logging_service.exception.ResourceNotFoundException;
import com.automobileservice.time_logging_service.repository.TimeLogRepository;
import com.automobileservice.time_logging_service.service.TimeLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
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
    public List<TimeLogResponse> getTimeLogsByEmployee(Long employeeId) {
        List<TimeLog> timeLogs = timeLogRepository.findByEmployeeIdOrderByLoggedAtDesc(employeeId);
        return timeLogs.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TimeLogResponse> getTimeLogsByProject(UUID projectId) {
        List<TimeLog> timeLogs = timeLogRepository.findByProjectIdOrderByLoggedAtDesc(projectId);
        return timeLogs.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TimeLogResponse> getTimeLogsByTask(UUID taskId) {
        List<TimeLog> timeLogs = timeLogRepository.findByTaskIdOrderByLoggedAtDesc(taskId);
        return timeLogs.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalHoursByEmployee(Long employeeId) {
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
    public TimeLogResponse approveTimeLog(UUID id, Long approvedBy) {
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
    public TimeLogResponse rejectTimeLog(UUID id, Long rejectedBy, String reason) {
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
        
        // Fetch employee name from auth-service
        try {
            var user = authServiceClient.getUserById(timeLog.getEmployeeId());
            if (user != null && user.getFirstName() != null && user.getLastName() != null) {
                response.setEmployeeName(user.getFirstName() + " " + user.getLastName());
            } else if (user != null && user.getUserName() != null) {
                response.setEmployeeName(user.getUserName());
            } else {
                response.setEmployeeName("Unknown");
            }
        } catch (Exception e) {
            log.warn("Failed to fetch employee name for ID {}: {}", timeLog.getEmployeeId(), e.getMessage());
            response.setEmployeeName("Unknown");
        }
        
        // Fetch project title from project-service
        if (timeLog.getProjectId() != null) {
            try {
                var project = projectServiceClient.getProjectById(timeLog.getProjectId());
                if (project != null && project.getTitle() != null) {
                    response.setProjectTitle(project.getTitle());
                } else {
                    response.setProjectTitle("Unknown Project");
                }
            } catch (Exception e) {
                log.warn("Failed to fetch project title for ID {}: {}", timeLog.getProjectId(), e.getMessage());
                response.setProjectTitle("Unknown Project");
            }
        }
        
        // Fetch task name from project-service
        if (timeLog.getTaskId() != null) {
            try {
                var task = projectServiceClient.getTaskByIdAlone(timeLog.getTaskId());
                if (task != null && task.getTitle() != null) {
                    response.setTaskName(task.getTitle());
                } else {
                    response.setTaskName("Unknown Task");
                }
            } catch (Exception e) {
                log.warn("Failed to fetch task name for ID {}: {}", timeLog.getTaskId(), e.getMessage());
                response.setTaskName("Unknown Task");
            }
        }
        
        return response;
    }
    
    @Override
    public EmployeeSummaryResponse getEmployeeSummary(Long employeeId) {
        log.info("Getting employee summary for: {}", employeeId);
        
        BigDecimal totalHours = timeLogRepository.getTotalHoursByEmployee(employeeId);
        BigDecimal approvedHours = timeLogRepository.getTotalApprovedHoursByEmployee(employeeId);
        
        List<TimeLog> pendingLogs = timeLogRepository.findByEmployeeIdAndApprovalStatusOrderByLoggedAtDesc(
            employeeId, "PENDING");
        BigDecimal pendingHours = pendingLogs.stream()
            .map(TimeLog::getHours)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Default hourly rate - this should come from employee service in real scenario
        BigDecimal hourlyRate = new BigDecimal("50.00");
        BigDecimal totalEarnings = approvedHours.multiply(hourlyRate);
        
        return EmployeeSummaryResponse.builder()
            .employeeId(employeeId.toString())
            .employeeName("Employee " + employeeId) // TODO: Fetch from auth service
            .department("Engineering") // TODO: Fetch from auth service
            .totalHoursLogged(totalHours)
            .hourlyRate(hourlyRate)
            .totalEarnings(totalEarnings)
            .build();
    }
    
    @Override
    public WeeklySummaryResponse getWeeklySummary(Long employeeId) {
        log.info("Getting weekly summary for employee: {}", employeeId);
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfWeek = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            .withHour(23).withMinute(59).withSecond(59);
        
        List<TimeLog> weekLogs = timeLogRepository.findByEmployeeIdAndLoggedAtBetween(
            employeeId, startOfWeek, endOfWeek);
        
        // Group by day of week
        Map<DayOfWeek, BigDecimal> hoursByDay = new HashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            hoursByDay.put(day, BigDecimal.ZERO);
        }
        
        for (TimeLog log : weekLogs) {
            DayOfWeek day = log.getLoggedAt().getDayOfWeek();
            hoursByDay.put(day, hoursByDay.get(day).add(log.getHours()));
        }
        
        List<WeeklySummaryResponse.DailyHours> dailyHours = hoursByDay.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> WeeklySummaryResponse.DailyHours.builder()
                .day(entry.getKey().getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                .hours(entry.getValue())
                .build())
            .collect(Collectors.toList());
        
        // Group by project
        Map<UUID, List<TimeLog>> logsByProject = weekLogs.stream()
            .collect(Collectors.groupingBy(TimeLog::getProjectId));
        
        List<WeeklySummaryResponse.ProjectBreakdown> projectBreakdown = logsByProject.entrySet().stream()
            .map(entry -> {
                UUID projectId = entry.getKey();
                List<TimeLog> logs = entry.getValue();
                BigDecimal totalHours = logs.stream()
                    .map(TimeLog::getHours)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                return WeeklySummaryResponse.ProjectBreakdown.builder()
                    .projectId(projectId.toString())
                    .projectTitle("Project " + projectId.toString().substring(0, 8)) // TODO: Fetch from project service
                    .taskCount((int) logs.stream().map(TimeLog::getTaskId).distinct().count())
                    .totalHours(totalHours)
                    .build();
            })
            .collect(Collectors.toList());
        
        return WeeklySummaryResponse.builder()
            .dailyHours(dailyHours)
            .projectBreakdown(projectBreakdown)
            .build();
    }
    
    @Override
    public List<SmartSuggestionResponse> getSmartSuggestions(Long employeeId) {
        log.info("Getting smart suggestions for employee: {}", employeeId);
        
        List<SmartSuggestionResponse> suggestions = new ArrayList<>();
        
        // Note: Smart suggestions require task data from project-service
        // This is a basic implementation that returns helpful hints without task data
        // Frontend should handle cases where suggestions might be empty
        
        // Suggestion 1: Check for recent activity
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        List<TimeLog> recentLogs = timeLogRepository.findByEmployeeIdAndLoggedAtAfter(employeeId, weekAgo);
        
        // Suggestion 2: Check pending approvals
        List<TimeLog> pendingLogs = timeLogRepository.findByEmployeeIdAndApprovalStatusOrderByLoggedAtDesc(
            employeeId, "PENDING");
        
        if (pendingLogs.size() > 5) {
            // Create a dummy task response for display purposes
            TaskResponse dummyTask = TaskResponse.builder()
                .id("pending-logs")
                .taskName("Review Pending Time Logs")
                .description("You have pending time logs awaiting approval")
                .status("PENDING")
                .build();
                
            suggestions.add(SmartSuggestionResponse.builder()
                .task(dummyTask)
                .projectTitle("Time Log Management")
                .reason("You have " + pendingLogs.size() + " pending time logs awaiting approval.")
                .urgency("medium")
                .icon("deadline")
                .build());
        }
        
        if (recentLogs.isEmpty()) {
            TaskResponse dummyTask = TaskResponse.builder()
                .id("log-hours")
                .taskName("Log Your Work Hours")
                .description("Keep your time tracking up to date")
                .status("TODO")
                .build();
                
            suggestions.add(SmartSuggestionResponse.builder()
                .task(dummyTask)
                .projectTitle("All Projects")
                .reason("No time logs in the past 7 days. Consider logging your work hours.")
                .urgency("high")
                .icon("progress")
                .build());
        }
        
        // TODO: Integrate with project-service to fetch actual tasks with approaching deadlines
        // This would require calling projectServiceClient.getTasksByEmployee(employeeId)
        // and filtering by deadline dates
        
        return suggestions;
    }
    
    @Override
    public EfficiencyMetricsResponse getEfficiencyMetrics(Long employeeId) {
        log.info("Getting efficiency metrics for employee: {}", employeeId);
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekAgo = now.minusDays(7);
        LocalDateTime twoWeeksAgo = now.minusDays(14);
        
        // Current week hours
        List<TimeLog> currentWeekLogs = timeLogRepository.findByEmployeeIdAndLoggedAtBetween(
            employeeId, weekAgo, now);
        BigDecimal currentWeekHours = currentWeekLogs.stream()
            .map(TimeLog::getHours)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Previous week hours
        List<TimeLog> previousWeekLogs = timeLogRepository.findByEmployeeIdAndLoggedAtBetween(
            employeeId, twoWeeksAgo, weekAgo);
        BigDecimal previousWeekHours = previousWeekLogs.stream()
            .map(TimeLog::getHours)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Calculate trend
        BigDecimal weeklyTrend = BigDecimal.ZERO;
        if (previousWeekHours.compareTo(BigDecimal.ZERO) > 0) {
            weeklyTrend = currentWeekHours.subtract(previousWeekHours)
                .divide(previousWeekHours, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        }
        
        // Calculate efficiency breakdown
        List<TimeLog> allLogs = timeLogRepository.findByEmployeeIdOrderByLoggedAtDesc(employeeId);
        int totalTasks = (int) allLogs.stream().map(TimeLog::getTaskId).distinct().count();
        
        // Simple metrics calculation
        BigDecimal avgTaskTime = totalTasks > 0 
            ? timeLogRepository.getTotalHoursByEmployee(employeeId)
                .divide(new BigDecimal(totalTasks), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        
        EfficiencyMetricsResponse.EfficiencyBreakdown breakdown = EfficiencyMetricsResponse.EfficiencyBreakdown.builder()
            .onTime(new BigDecimal("85.0")) // TODO: Calculate from actual task completion data
            .overEstimate(new BigDecimal("15.0")) // TODO: Calculate from task estimates
            .avgTaskTime(avgTaskTime)
            .build();
        
        // Overall efficiency score (placeholder calculation)
        BigDecimal efficiency = new BigDecimal("85.5");
        
        return EfficiencyMetricsResponse.builder()
            .efficiency(efficiency)
            .weeklyTrend(weeklyTrend)
            .breakdown(breakdown)
            .build();
    }
    
    @Override
    public List<TimeLogResponse> getTimeLogsByEmployeeAndProject(Long employeeId, UUID projectId) {
        log.info("Getting time logs for employee: {} and project: {}", employeeId, projectId);
        
        List<TimeLog> timeLogs = timeLogRepository.findByEmployeeIdAndProjectIdOrderByLoggedAtDesc(
            employeeId, projectId);
        
        return timeLogs.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
}
