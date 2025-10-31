package com.automobileservice.time_logging_service.repository;

import com.automobileservice.time_logging_service.entity.TimeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface TimeLogRepository extends JpaRepository<TimeLog, String> {
    
    // Find all time logs for a specific employee, ordered by most recent
    List<TimeLog> findByEmployeeUserIdOrderByLoggedAtDesc(String employeeId);
    
    // Find time logs for a specific project
    List<TimeLog> findByProjectIdOrderByLoggedAtDesc(String projectId);
    
    // Find time logs for a specific task
    List<TimeLog> findByTaskIdOrderByLoggedAtDesc(String taskId);
    
    // Calculate total hours logged by an employee
    @Query("SELECT COALESCE(SUM(t.hours), 0) FROM TimeLog t WHERE t.employee.userId = :employeeId")
    BigDecimal getTotalHoursByEmployee(@Param("employeeId") String employeeId);
    
    // Calculate total hours for a specific project
    @Query("SELECT COALESCE(SUM(t.hours), 0) FROM TimeLog t WHERE t.project.id = :projectId")
    BigDecimal getTotalHoursByProject(@Param("projectId") String projectId);
    
    // Calculate total hours for a specific task
    @Query("SELECT COALESCE(SUM(t.hours), 0) FROM TimeLog t WHERE t.task.id = :taskId")
    BigDecimal getTotalHoursByTask(@Param("taskId") String taskId);
    
    // Find time logs by employee and project
    List<TimeLog> findByEmployeeUserIdAndProjectIdOrderByLoggedAtDesc(
        String employeeId, String projectId);
}