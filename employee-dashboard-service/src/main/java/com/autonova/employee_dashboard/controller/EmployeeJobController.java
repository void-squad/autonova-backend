package com.autonova.employee_dashboard.controller;

import com.autonova.employee_dashboard.dto.JobResponse;
import com.autonova.employee_dashboard.dto.UpdateJobStatusRequest;
import com.autonova.employee_dashboard.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/employee")
@RequiredArgsConstructor
public class EmployeeJobController {

    private final JobService jobService;

    /**
     * GET /api/employee/jobs
     * Get all jobs (optionally filter by employeeId)
     */
    @GetMapping("/jobs")
    public ResponseEntity<List<JobResponse>> getAllJobs(
            @RequestParam(required = false) UUID employeeId) {
        List<JobResponse> jobs = jobService.getAllJobs(employeeId);
        return ResponseEntity.ok(jobs);
    }

    /**
     * GET /api/employee/jobs/active
     * Get all active jobs (optionally filter by employeeId)
     */
    @GetMapping("/jobs/active")
    public ResponseEntity<List<JobResponse>> getActiveJobs(
            @RequestParam(required = false) UUID employeeId) {
        List<JobResponse> jobs = jobService.getActiveJobs(employeeId);
        return ResponseEntity.ok(jobs);
    }

    /**
     * POST /api/employee/jobs/{jobId}/start
     * Start a job (change status from PENDING or PAUSED to IN_PROGRESS)
     */
    @PostMapping("/jobs/{jobId}/start")
    public ResponseEntity<JobResponse> startJob(@PathVariable UUID jobId) {
        JobResponse job = jobService.startJob(jobId);
        return ResponseEntity.ok(job);
    }

    /**
     * PATCH /api/employee/jobs/{jobId}/status
     * Update job status with custom status value
     */
    @PatchMapping("/jobs/{jobId}/status")
    public ResponseEntity<JobResponse> updateJobStatus(
            @PathVariable UUID jobId,
            @Valid @RequestBody UpdateJobStatusRequest request) {
        JobResponse job = jobService.updateJobStatus(jobId, request);
        return ResponseEntity.ok(job);
    }

    /**
     * POST /api/employee/jobs/{jobId}/pause
     * Pause a job (change status from IN_PROGRESS to PAUSED)
     */
    @PostMapping("/jobs/{jobId}/pause")
    public ResponseEntity<JobResponse> pauseJob(@PathVariable UUID jobId) {
        JobResponse job = jobService.pauseJob(jobId);
        return ResponseEntity.ok(job);
    }

    /**
     * POST /api/employee/jobs/{jobId}/stop
     * Stop a job (change status to STOPPED)
     */
    @PostMapping("/jobs/{jobId}/stop")
    public ResponseEntity<JobResponse> stopJob(@PathVariable UUID jobId) {
        JobResponse job = jobService.stopJob(jobId);
        return ResponseEntity.ok(job);
    }

    /**
     * GET /api/employee/jobs/{jobId}
     * Get a specific job by ID
     */
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<JobResponse> getJobById(@PathVariable UUID jobId) {
        JobResponse job = jobService.getJobById(jobId);
        return ResponseEntity.ok(job);
    }
}
