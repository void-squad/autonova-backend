package com.automobileservice.time_logging_service.service;

import com.automobileservice.time_logging_service.dto.request.TimeLogRequest;
import com.automobileservice.time_logging_service.dto.response.*;

import java.math.BigDecimal;
import java.util.List;

public interface TimeLogService {
    
    /**
     * Create a new time log entry
     * @param request Time log details
     * @return Created time log
     */
    TimeLogResponse createTimeLog(TimeLogRequest request);
    
    /**
     * Update an existing time log
     * @param id Time log ID
     * @param request Updated details
     * @return Updated time log
     */
    TimeLogResponse updateTimeLog(String id, TimeLogRequest request);
    
    /**
     * Delete a time log entry
     * @param id Time log ID
     */
    void deleteTimeLog(String id);
    
    /**
     * Get time log by ID
     * @param id Time log ID
     * @return Time log details
     */
    TimeLogResponse getTimeLogById(String id);
    
    /**
     * Get all time logs for a specific employee
     * @param employeeId Employee ID
     * @return List of time logs
     */
    List<TimeLogResponse> getTimeLogsByEmployee(String employeeId);
    
    /**
     * Get all time logs for a specific project
     * @param projectId Project ID
     * @return List of time logs
     */
    List<TimeLogResponse> getTimeLogsByProject(String projectId);
    
    /**
     * Get all time logs for a specific task
     * @param taskId Task ID
     * @return List of time logs
     */
    List<TimeLogResponse> getTimeLogsByTask(String taskId);
    
    /**
     * Get total hours logged by an employee
     * @param employeeId Employee ID
     * @return Total hours
     */
    BigDecimal getTotalHoursByEmployee(String employeeId);
    
    /**
     * Get employee summary with total hours and earnings
     * @param employeeId Employee ID
     * @return Employee summary
     */
    EmployeeSummaryResponse getEmployeeSummary(String employeeId);
    
    /**
     * Get time logs for a specific employee on a specific project
     * @param employeeId Employee ID
     * @param projectId Project ID
     * @return List of time logs
     */
    List<TimeLogResponse> getTimeLogsByEmployeeAndProject(String employeeId, String projectId);
    
    /**
     * Get weekly summary for an employee
     * @param employeeId Employee ID
     * @return Weekly summary with daily hours and project breakdown
     */
    WeeklySummaryResponse getWeeklySummary(String employeeId);
    
    /**
     * Get smart task suggestions for an employee
     * @param employeeId Employee ID
     * @return List of suggested tasks with reasoning
     */
    List<SmartSuggestionResponse> getSmartSuggestions(String employeeId);
    
    /**
     * Get efficiency metrics for an employee
     * @param employeeId Employee ID
     * @return Efficiency metrics and productivity data
     */
    EfficiencyMetricsResponse getEfficiencyMetrics(String employeeId);
    
    /**
     * Get all time logs (for admin)
     * @return List of all time logs
     */
    List<TimeLogResponse> getAllTimeLogs();
    
    /**
     * Get all pending time logs (for admin approval)
     * @return List of pending time logs
     */
    List<TimeLogResponse> getPendingTimeLogs();
    
    /**
     * Approve a time log
     * @param id Time log ID
     * @return Updated time log
     */
    TimeLogResponse approveTimeLog(String id);
    
    /**
     * Reject a time log
     * @param id Time log ID
     * @param reason Rejection reason
     * @return Updated time log
     */
    TimeLogResponse rejectTimeLog(String id, String reason);
}