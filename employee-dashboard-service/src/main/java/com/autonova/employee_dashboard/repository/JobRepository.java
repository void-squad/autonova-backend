package com.autonova.employee_dashboard.repository;

import com.autonova.employee_dashboard.domain.entity.Job;
import com.autonova.employee_dashboard.domain.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {
    
    List<Job> findByEmployeeId(UUID employeeId);
    
    List<Job> findByEmployeeIdAndStatus(UUID employeeId, JobStatus status);
    
    List<Job> findByStatus(JobStatus status);
}
