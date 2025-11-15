package com.automobileservice.time_logging_service.controller;

import com.automobileservice.time_logging_service.dto.request.TimeLogRequest;
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
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/time-logs")
@RequiredArgsConstructor
@Slf4j
public class TimeLogController {
    
    private final TimeLogService timeLogService;
    
    @PostMapping
    public ResponseEntity<TimeLogResponse> createTimeLog(@Valid @RequestBody TimeLogRequest request) {
        log.info("REST request to create time log for employee: {}", request.getEmployeeId());
        TimeLogResponse response = timeLogService.createTimeLog(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<TimeLogResponse> updateTimeLog(
            @PathVariable UUID id,
            @Valid @RequestBody TimeLogRequest request) {
        log.info("REST request to update time log: {}", id);
        TimeLogResponse response = timeLogService.updateTimeLog(id, request);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTimeLog(@PathVariable UUID id) {
        log.info("REST request to delete time log: {}", id);
        timeLogService.deleteTimeLog(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<TimeLogResponse> getTimeLogById(@PathVariable UUID id) {
        log.info("REST request to get time log: {}", id);
        TimeLogResponse response = timeLogService.getTimeLogById(id);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<TimeLogResponse>> getTimeLogsByEmployee(@PathVariable String employeeId) {
        log.info("REST request to get time logs for employee: {}", employeeId);
        List<TimeLogResponse> response = timeLogService.getTimeLogsByEmployee(employeeId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<TimeLogResponse>> getTimeLogsByProject(@PathVariable String projectId) {
        log.info("REST request to get time logs for project: {}", projectId);
        List<TimeLogResponse> response = timeLogService.getTimeLogsByProject(projectId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/task/{taskId}")
    public ResponseEntity<List<TimeLogResponse>> getTimeLogsByTask(@PathVariable String taskId) {
        log.info("REST request to get time logs for task: {}", taskId);
        List<TimeLogResponse> response = timeLogService.getTimeLogsByTask(taskId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/employee/{employeeId}/total-hours")
    public ResponseEntity<BigDecimal> getTotalHoursByEmployee(@PathVariable String employeeId) {
        log.info("REST request to get total hours for employee: {}", employeeId);
        BigDecimal totalHours = timeLogService.getTotalHoursByEmployee(employeeId);
        return ResponseEntity.ok(totalHours);
    }
    
    @GetMapping("/pending")
    public ResponseEntity<List<TimeLogResponse>> getPendingTimeLogs() {
        log.info("REST request to get all pending time logs");
        List<TimeLogResponse> response = timeLogService.getPendingTimeLogs();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/all")
    public ResponseEntity<List<TimeLogResponse>> getAllTimeLogs() {
        log.info("REST request to get all time logs");
        List<TimeLogResponse> response = timeLogService.getAllTimeLogs();
        return ResponseEntity.ok(response);
    }
    
    @PatchMapping("/{id}/approve")
    public ResponseEntity<TimeLogResponse> approveTimeLog(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        String approvedBy = body != null ? body.get("approvedBy") : "ADMIN";
        log.info("REST request to approve time log: {} by user: {}", id, approvedBy);
        TimeLogResponse response = timeLogService.approveTimeLog(id, approvedBy);
        return ResponseEntity.ok(response);
    }
    
    @PatchMapping("/{id}/reject")
    public ResponseEntity<TimeLogResponse> rejectTimeLog(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        String rejectedBy = body.getOrDefault("rejectedBy", "ADMIN");
        String reason = body.getOrDefault("reason", "No reason provided");
        log.info("REST request to reject time log: {} by user: {} with reason: {}", id, rejectedBy, reason);
        TimeLogResponse response = timeLogService.rejectTimeLog(id, rejectedBy, reason);
        return ResponseEntity.ok(response);
    }
}
