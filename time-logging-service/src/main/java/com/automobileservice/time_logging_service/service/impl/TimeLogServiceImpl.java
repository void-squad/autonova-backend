package com.automobileservice.time_logging_service.service.impl;

import com.automobileservice.time_logging_service.dto.request.TimeLogRequest;
import com.automobileservice.time_logging_service.dto.response.*;
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
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
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

        // Prevent assigning to completed or cancelled projects
        if ("COMPLETED".equals(project.getStatus()) || "CANCELLED".equals(project.getStatus())) {
            throw new BusinessRuleException("Cannot assign time log to a completed or cancelled project");
        }

        ProjectTask task = projectTaskRepository.findById(request.getTaskId())
            .orElseThrow(() -> new ResourceNotFoundException("Task", "id", request.getTaskId()));

        // Ensure task belongs to project
        if (!task.getProject().getId().equals(project.getId())) {
            throw new BusinessRuleException("Task does not belong to the specified project");
        }

        // Ensure employee is assigned to the task
        if (task.getAssignedEmployee() == null ||
            !task.getAssignedEmployee().getUserId().equals(employee.getUserId())) {
            throw new BusinessRuleException("Employee is not assigned to this task");
        }

        // Keep reference to old task so we can recalculate its hours if task changed
        ProjectTask oldTask = timeLog.getTask();

        // Update fields
        timeLog.setEmployee(employee);
        timeLog.setProject(project);
        timeLog.setTask(task);
        timeLog.setHours(request.getHours());
        timeLog.setNote(request.getNote());
        
        TimeLog updatedTimeLog = timeLogRepository.save(timeLog);
        
        // Recalculate task actual hours for both old and new tasks (if different)
        if (oldTask != null && !oldTask.getId().equals(task.getId())) {
            updateTaskActualHours(oldTask);
        }
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
    
    @Override
    public WeeklySummaryResponse getWeeklySummary(String employeeId) {
        log.info("Getting weekly summary for employee: {}", employeeId);
        
        // Get time logs for the past 7 days
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<TimeLog> timeLogs = timeLogRepository.findByEmployeeUserIdAndLoggedAtAfter(employeeId, sevenDaysAgo);
        
        // Calculate daily hours
        Map<LocalDate, BigDecimal> dailyHoursMap = new HashMap<>();
        for (int i = 6; i >= 0; i--) {
            dailyHoursMap.put(LocalDate.now().minusDays(i), BigDecimal.ZERO);
        }
        
        for (TimeLog log : timeLogs) {
            LocalDate logDate = log.getLoggedAt().toLocalDate();
            dailyHoursMap.merge(logDate, log.getHours(), BigDecimal::add);
        }
        
        List<WeeklySummaryResponse.DailyHours> dailyHours = dailyHoursMap.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> WeeklySummaryResponse.DailyHours.builder()
                .day(entry.getKey().getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                .hours(entry.getValue())
                .build())
            .collect(Collectors.toList());
        
        // Calculate project breakdown
        Map<String, WeeklySummaryResponse.ProjectBreakdown> projectMap = new HashMap<>();
        
        for (TimeLog log : timeLogs) {
            String projectId = log.getProject().getId();
            projectMap.computeIfAbsent(projectId, id -> WeeklySummaryResponse.ProjectBreakdown.builder()
                .projectId(id)
                .projectTitle(log.getProject().getTitle())
                .taskCount(0)
                .totalHours(BigDecimal.ZERO)
                .build());
            
            WeeklySummaryResponse.ProjectBreakdown breakdown = projectMap.get(projectId);
            breakdown.setTotalHours(breakdown.getTotalHours().add(log.getHours()));
            breakdown.setTaskCount(breakdown.getTaskCount() + 1);
        }
        
        List<WeeklySummaryResponse.ProjectBreakdown> projectBreakdown = new ArrayList<>(projectMap.values());
        
        return WeeklySummaryResponse.builder()
            .dailyHours(dailyHours)
            .projectBreakdown(projectBreakdown)
            .build();
    }
    
    @Override
    public List<SmartSuggestionResponse> getSmartSuggestions(String employeeId) {
        log.info("Getting smart suggestions for employee: {}", employeeId);
        
        // Get incomplete tasks assigned to the employee
        List<ProjectTask> incompleteTasks = projectTaskRepository.findIncompleteTasksByEmployee(employeeId);
        
        List<SmartSuggestionResponse> suggestions = new ArrayList<>();
        
        for (ProjectTask task : incompleteTasks) {
            String urgency = "low";
            String reason = "Recommended based on your schedule";
            String icon = "efficiency";
            
            // Check due date
            if (task.getDueDate() != null) {
                long daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), task.getDueDate());
                
                if (daysUntilDue <= 1) {
                    urgency = "high";
                    reason = daysUntilDue == 0 ? "Due today" : "Due tomorrow";
                    icon = "deadline";
                } else if (daysUntilDue <= 3) {
                    urgency = "medium";
                    reason = "Due in " + daysUntilDue + " days";
                    icon = "deadline";
                }
            }
            
            // Check priority
            if ("HIGH".equals(task.getPriority())) {
                urgency = "high".equals(urgency) ? "high" : "medium";
                icon = "priority";
                reason = "High priority task";
            }
            
            // Check if in progress
            if ("IN_PROGRESS".equals(task.getStatus()) && task.getActualHours().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal progress = task.getEstimatedHours() != null && task.getEstimatedHours().compareTo(BigDecimal.ZERO) > 0
                    ? task.getActualHours().divide(task.getEstimatedHours(), 2, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                    : BigDecimal.ZERO;
                reason = "Already started - " + progress.intValue() + "% complete";
                icon = "progress";
            }
            
            TaskResponse taskResponse = TaskResponse.builder()
                .id(task.getId())
                .projectId(task.getProject().getId())
                .taskName(task.getTaskName())
                .description(task.getDescription())
                .assignedEmployeeId(task.getAssignedEmployee() != null ? task.getAssignedEmployee().getUserId() : null)
                .assignedEmployeeName(task.getAssignedEmployee() != null ? 
                    task.getAssignedEmployee().getUser().getFirstName() + " " + 
                    task.getAssignedEmployee().getUser().getLastName() : null)
                .estimatedHours(task.getEstimatedHours())
                .actualHours(task.getActualHours())
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .build();
            
            suggestions.add(SmartSuggestionResponse.builder()
                .task(taskResponse)
                .projectTitle(task.getProject().getTitle())
                .reason(reason)
                .urgency(urgency)
                .icon(icon)
                .build());
        }
        
        // Sort by urgency: high > medium > low
        suggestions.sort((a, b) -> {
            Map<String, Integer> urgencyOrder = Map.of("high", 3, "medium", 2, "low", 1);
            return urgencyOrder.get(b.getUrgency()).compareTo(urgencyOrder.get(a.getUrgency()));
        });
        
        // Return top 3
        return suggestions.stream().limit(3).collect(Collectors.toList());
    }
    
    @Override
    public EfficiencyMetricsResponse getEfficiencyMetrics(String employeeId) {
        log.info("Getting efficiency metrics for employee: {}", employeeId);
        
        // Get completed tasks in the past month
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        List<TimeLog> timeLogs = timeLogRepository.findByEmployeeUserIdAndLoggedAtAfter(employeeId, oneMonthAgo);
        
        // Get unique completed tasks
        Set<String> completedTaskIds = timeLogs.stream()
            .map(log -> log.getTask().getId())
            .collect(Collectors.toSet());
        
        List<ProjectTask> completedTasks = projectTaskRepository.findAllById(completedTaskIds).stream()
            .filter(task -> "COMPLETED".equals(task.getStatus()))
            .collect(Collectors.toList());
        
        if (completedTasks.isEmpty()) {
            // Return default metrics if no data
            return EfficiencyMetricsResponse.builder()
                .efficiency(new BigDecimal("75"))
                .weeklyTrend(BigDecimal.ZERO)
                .breakdown(EfficiencyMetricsResponse.EfficiencyBreakdown.builder()
                    .onTime(new BigDecimal("80"))
                    .overEstimate(new BigDecimal("20"))
                    .avgTaskTime(new BigDecimal("2.5"))
                    .build())
                .build();
        }
        
        // Calculate metrics
        long onTimeCount = completedTasks.stream()
            .filter(task -> task.getDueDate() == null || 
                !task.getUpdatedAt().toLocalDate().isAfter(task.getDueDate()))
            .count();
        
        BigDecimal onTimePercentage = new BigDecimal(onTimeCount)
            .divide(new BigDecimal(completedTasks.size()), 2, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
        
        long overEstimateCount = completedTasks.stream()
            .filter(task -> task.getEstimatedHours() != null && 
                task.getActualHours().compareTo(task.getEstimatedHours()) > 0)
            .count();
        
        BigDecimal overEstimatePercentage = new BigDecimal(overEstimateCount)
            .divide(new BigDecimal(completedTasks.size()), 2, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
        
        BigDecimal totalHours = completedTasks.stream()
            .map(ProjectTask::getActualHours)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal avgTaskTime = totalHours.divide(new BigDecimal(completedTasks.size()), 2, RoundingMode.HALF_UP);
        
        // Calculate overall efficiency (weighted average of on-time and under-estimate)
        BigDecimal efficiency = onTimePercentage.multiply(new BigDecimal("0.6"))
            .add(new BigDecimal("100").subtract(overEstimatePercentage).multiply(new BigDecimal("0.4")));
        
        // Calculate weekly trend (compare this week vs last week)
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        LocalDateTime twoWeeksAgo = LocalDateTime.now().minusWeeks(2);
        
        List<TimeLog> thisWeekLogs = timeLogRepository.findByEmployeeUserIdAndLoggedAtBetween(employeeId, oneWeekAgo, LocalDateTime.now());
        List<TimeLog> lastWeekLogs = timeLogRepository.findByEmployeeUserIdAndLoggedAtBetween(employeeId, twoWeeksAgo, oneWeekAgo);
        
        BigDecimal thisWeekHours = thisWeekLogs.stream()
            .map(TimeLog::getHours)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal lastWeekHours = lastWeekLogs.stream()
            .map(TimeLog::getHours)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal weeklyTrend = lastWeekHours.compareTo(BigDecimal.ZERO) > 0
            ? thisWeekHours.subtract(lastWeekHours)
                .divide(lastWeekHours, 2, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
            : BigDecimal.ZERO;
        
        return EfficiencyMetricsResponse.builder()
            .efficiency(efficiency.setScale(0, RoundingMode.HALF_UP))
            .weeklyTrend(weeklyTrend)
            .breakdown(EfficiencyMetricsResponse.EfficiencyBreakdown.builder()
                .onTime(onTimePercentage)
                .overEstimate(overEstimatePercentage)
                .avgTaskTime(avgTaskTime)
                .build())
            .build();
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