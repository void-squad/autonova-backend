package com.automobileservice.time_logging_service.service;

import com.automobileservice.time_logging_service.dto.request.TimeLogRequest;
import com.automobileservice.time_logging_service.dto.response.EmployeeSummaryResponse;
import com.automobileservice.time_logging_service.dto.response.TimeLogResponse;

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
}