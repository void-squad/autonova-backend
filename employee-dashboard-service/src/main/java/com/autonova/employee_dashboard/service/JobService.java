package com.autonova.employee_dashboard.service;

import com.autonova.employee_dashboard.domain.entity.Job;
import com.autonova.employee_dashboard.domain.enums.JobStatus;
import com.autonova.employee_dashboard.dto.JobResponse;
import com.autonova.employee_dashboard.dto.UpdateJobStatusRequest;
import com.autonova.employee_dashboard.exception.JobNotFoundException;
import com.autonova.employee_dashboard.exception.InvalidJobStateException;
import com.autonova.employee_dashboard.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;

    public List<JobResponse> getAllJobs(UUID employeeId) {
        List<Job> jobs = employeeId != null 
            ? jobRepository.findByEmployeeId(employeeId)
            : jobRepository.findAll();
        
        return jobs.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    public List<JobResponse> getActiveJobs(UUID employeeId) {
        List<Job> jobs = employeeId != null
            ? jobRepository.findByEmployeeIdAndStatus(employeeId, JobStatus.ACTIVE)
            : jobRepository.findByStatus(JobStatus.ACTIVE);
        
        return jobs.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public JobResponse startJob(UUID jobId) {
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new JobNotFoundException("Job not found with id: " + jobId));

        if (job.getStatus() != JobStatus.PENDING && job.getStatus() != JobStatus.PAUSED) {
            throw new InvalidJobStateException("Job can only be started from PENDING or PAUSED status");
        }

        job.setStatus(JobStatus.IN_PROGRESS);
        if (job.getStartedAt() == null) {
            job.setStartedAt(LocalDateTime.now());
        }
        job.setPausedAt(null);

        Job savedJob = jobRepository.save(job);
        return mapToResponse(savedJob);
    }

    @Transactional
    public JobResponse updateJobStatus(UUID jobId, UpdateJobStatusRequest request) {
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new JobNotFoundException("Job not found with id: " + jobId));

        job.setStatus(request.getStatus());
        
        // Update timestamps based on status
        switch (request.getStatus()) {
            case IN_PROGRESS:
                if (job.getStartedAt() == null) {
                    job.setStartedAt(LocalDateTime.now());
                }
                break;
            case PAUSED:
                job.setPausedAt(LocalDateTime.now());
                break;
            case COMPLETED:
            case STOPPED:
                job.setCompletedAt(LocalDateTime.now());
                break;
        }

        Job savedJob = jobRepository.save(job);
        return mapToResponse(savedJob);
    }

    @Transactional
    public JobResponse pauseJob(UUID jobId) {
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new JobNotFoundException("Job not found with id: " + jobId));

        if (job.getStatus() != JobStatus.IN_PROGRESS) {
            throw new InvalidJobStateException("Only IN_PROGRESS jobs can be paused");
        }

        job.setStatus(JobStatus.PAUSED);
        job.setPausedAt(LocalDateTime.now());

        Job savedJob = jobRepository.save(job);
        return mapToResponse(savedJob);
    }

    @Transactional
    public JobResponse stopJob(UUID jobId) {
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new JobNotFoundException("Job not found with id: " + jobId));

        if (job.getStatus() == JobStatus.COMPLETED || job.getStatus() == JobStatus.STOPPED) {
            throw new InvalidJobStateException("Job is already completed or stopped");
        }

        job.setStatus(JobStatus.STOPPED);
        job.setCompletedAt(LocalDateTime.now());

        Job savedJob = jobRepository.save(job);
        return mapToResponse(savedJob);
    }

    public JobResponse getJobById(UUID jobId) {
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new JobNotFoundException("Job not found with id: " + jobId));
        
        return mapToResponse(job);
    }

    private JobResponse mapToResponse(Job job) {
        return JobResponse.builder()
            .id(job.getId())
            .title(job.getTitle())
            .description(job.getDescription())
            .projectId(job.getProjectId())
            .employeeId(job.getEmployeeId())
            .status(job.getStatus())
            .startedAt(job.getStartedAt())
            .pausedAt(job.getPausedAt())
            .completedAt(job.getCompletedAt())
            .estimatedHours(job.getEstimatedHours())
            .actualHours(job.getActualHours())
            .createdAt(job.getCreatedAt())
            .updatedAt(job.getUpdatedAt())
            .build();
    }
}
