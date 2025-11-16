package com.automobileservice.time_logging_service.service;

import com.automobileservice.time_logging_service.dto.request.TimeLogRequest;
import com.automobileservice.time_logging_service.dto.response.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

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
    TimeLogResponse updateTimeLog(UUID id, TimeLogRequest request);
    
    /**
     * Delete a time log entry
     * @param id Time log ID
     */
    void deleteTimeLog(UUID id);
    
    /**
     * Get time log by ID
     * @param id Time log ID
     * @return Time log details
     */
    TimeLogResponse getTimeLogById(UUID id);
    
    /**
     * Get all time logs for a specific employee
     * @param employeeId Employee ID
     * @return List of time logs
     */
    List<TimeLogResponse> getTimeLogsByEmployee(Long employeeId);
    
    /**
     * Get all time logs for a specific project
     * @param projectId Project ID
     * @return List of time logs
     */
    List<TimeLogResponse> getTimeLogsByProject(UUID projectId);
    
    /**
     * Get all time logs for a specific task
     * @param taskId Task ID
     * @return List of time logs
     */
    List<TimeLogResponse> getTimeLogsByTask(UUID taskId);
    
    /**
     * Get total hours logged by an employee
     * @param employeeId Employee ID
     * @return Total hours
     */
    BigDecimal getTotalHoursByEmployee(Long employeeId);
    
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
     * @param approvedBy User ID who approved
     * @return Updated time log
     */
    TimeLogResponse approveTimeLog(UUID id, Long approvedBy);
    
    /**
     * Reject a time log
     * @param id Time log ID
     * @param rejectedBy User ID who rejected
     * @param reason Rejection reason
     * @return Updated time log
     */
    TimeLogResponse rejectTimeLog(UUID id, Long rejectedBy, String reason);
    
    /**
     * Get employee summary (total hours, earnings, etc.)
     * @param employeeId Employee ID
     * @return Employee summary
     */
    EmployeeSummaryResponse getEmployeeSummary(Long employeeId);
    
    /**
     * Get weekly summary for an employee
     * @param employeeId Employee ID
     * @return Weekly summary with daily hours and project breakdown
     */
    WeeklySummaryResponse getWeeklySummary(Long employeeId);
    
    /**
     * Get smart suggestions for an employee
     * @param employeeId Employee ID
     * @return List of smart suggestions
     */
    List<SmartSuggestionResponse> getSmartSuggestions(Long employeeId);
    
    /**
     * Get efficiency metrics for an employee
     * @param employeeId Employee ID
     * @return Efficiency metrics
     */
    EfficiencyMetricsResponse getEfficiencyMetrics(Long employeeId);
    
    /**
     * Get time logs for a specific employee and project
     * @param employeeId Employee ID
     * @param projectId Project ID
     * @return List of time logs
     */
    List<TimeLogResponse> getTimeLogsByEmployeeAndProject(Long employeeId, UUID projectId);
}