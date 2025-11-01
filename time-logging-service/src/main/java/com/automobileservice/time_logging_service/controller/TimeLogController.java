package com.automobileservice.time_logging_service.controller;

import com.automobileservice.time_logging_service.dto.request.TimeLogRequest;
import com.automobileservice.time_logging_service.dto.response.EmployeeSummaryResponse;
import com.automobileservice.time_logging_service.dto.response.TimeLogResponse;
import com.automobileservice.time_logging_service.service.TimeLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/time-logs")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // Proper CORS will be Configured in production
public class TimeLogController {
    
    private final TimeLogService timeLogService;
    
    /**
     * Create a new time log entry
     * POST /api/time-logs
     */
    @PostMapping
    public ResponseEntity<TimeLogResponse> createTimeLog(@Valid @RequestBody TimeLogRequest request) {
        log.info("REST request to create time log for employee: {}", request.getEmployeeId());
        TimeLogResponse response = timeLogService.createTimeLog(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Update an existing time log
     * PUT /api/time-logs/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<TimeLogResponse> updateTimeLog(
            @PathVariable String id,
            @Valid @RequestBody TimeLogRequest request) {
        log.info("REST request to update time log: {}", id);
        TimeLogResponse response = timeLogService.updateTimeLog(id, request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Delete a time log entry
     * DELETE /api/time-logs/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTimeLog(@PathVariable String id) {
        log.info("REST request to delete time log: {}", id);
        timeLogService.deleteTimeLog(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Get a specific time log by ID
     * GET /api/time-logs/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<TimeLogResponse> getTimeLogById(@PathVariable String id) {
        log.info("REST request to get time log: {}", id);
        TimeLogResponse response = timeLogService.getTimeLogById(id);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get all time logs for a specific employee
     * GET /api/time-logs/employee/{employeeId}
     */
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<TimeLogResponse>> getTimeLogsByEmployee(@PathVariable String employeeId) {
        log.info("REST request to get time logs for employee: {}", employeeId);
        List<TimeLogResponse> response = timeLogService.getTimeLogsByEmployee(employeeId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get all time logs for a specific project
     * GET /api/time-logs/project/{projectId}
     */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<TimeLogResponse>> getTimeLogsByProject(@PathVariable String projectId) {
        log.info("REST request to get time logs for project: {}", projectId);
        List<TimeLogResponse> response = timeLogService.getTimeLogsByProject(projectId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get all time logs for a specific task
     * GET /api/time-logs/task/{taskId}
     */
    @GetMapping("/task/{taskId}")
    public ResponseEntity<List<TimeLogResponse>> getTimeLogsByTask(@PathVariable String taskId) {
        log.info("REST request to get time logs for task: {}", taskId);
        List<TimeLogResponse> response = timeLogService.getTimeLogsByTask(taskId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get total hours logged by an employee
     * GET /api/time-logs/employee/{employeeId}/total-hours
     */
    @GetMapping("/employee/{employeeId}/total-hours")
    public ResponseEntity<BigDecimal> getTotalHoursByEmployee(@PathVariable String employeeId) {
        log.info("REST request to get total hours for employee: {}", employeeId);
        BigDecimal totalHours = timeLogService.getTotalHoursByEmployee(employeeId);
        return ResponseEntity.ok(totalHours);
    }
    
    /**
     * Get employee summary with total hours and earnings
     * GET /api/time-logs/employee/{employeeId}/summary
     */
    @GetMapping("/employee/{employeeId}/summary")
    public ResponseEntity<EmployeeSummaryResponse> getEmployeeSummary(@PathVariable String employeeId) {
        log.info("REST request to get summary for employee: {}", employeeId);
        EmployeeSummaryResponse response = timeLogService.getEmployeeSummary(employeeId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get time logs for a specific employee on a specific project
     * GET /api/time-logs/employee/{employeeId}/project/{projectId}
     */
    @GetMapping("/employee/{employeeId}/project/{projectId}")
    public ResponseEntity<List<TimeLogResponse>> getTimeLogsByEmployeeAndProject(
            @PathVariable String employeeId,
            @PathVariable String projectId) {
        log.info("REST request to get time logs for employee: {} on project: {}", employeeId, projectId);
        List<TimeLogResponse> response = timeLogService.getTimeLogsByEmployeeAndProject(employeeId, projectId);
        return ResponseEntity.ok(response);
    }
}