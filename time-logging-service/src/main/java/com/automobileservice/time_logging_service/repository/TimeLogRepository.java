package com.automobileservice.time_logging_service.repository;

import com.automobileservice.time_logging_service.entity.TimeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    
    // Find time logs after a specific date for an employee
    List<TimeLog> findByEmployeeUserIdAndLoggedAtAfter(String employeeId, LocalDateTime after);
    
    // Find time logs between two dates for an employee
    List<TimeLog> findByEmployeeUserIdAndLoggedAtBetween(
        String employeeId, LocalDateTime start, LocalDateTime end);
    
    // Find all pending time logs (for admin approval)
    List<TimeLog> findByApprovalStatusOrderByLoggedAtDesc(String approvalStatus);
    
    // Find pending time logs for a specific employee
    List<TimeLog> findByEmployeeUserIdAndApprovalStatusOrderByLoggedAtDesc(
        String employeeId, String approvalStatus);
    
    // Calculate total hours by employee (approved only)
    @Query("SELECT COALESCE(SUM(t.hours), 0) FROM TimeLog t WHERE t.employee.userId = :employeeId AND t.approvalStatus = 'APPROVED'")
    BigDecimal getTotalApprovedHoursByEmployee(@Param("employeeId") String employeeId);
    
    // Calculate total hours for a task (approved only)
    @Query("SELECT COALESCE(SUM(t.hours), 0) FROM TimeLog t WHERE t.task.id = :taskId AND t.approvalStatus = 'APPROVED'")
    BigDecimal getTotalApprovedHoursByTask(@Param("taskId") String taskId);
    
    // Find approved time logs after a specific date for an employee
    @Query("SELECT t FROM TimeLog t WHERE t.employee.userId = :employeeId AND t.loggedAt > :after AND t.approvalStatus = 'APPROVED'")
    List<TimeLog> findApprovedByEmployeeUserIdAndLoggedAtAfter(
        @Param("employeeId") String employeeId, @Param("after") LocalDateTime after);
    
    // Find approved time logs between two dates for an employee
    @Query("SELECT t FROM TimeLog t WHERE t.employee.userId = :employeeId AND t.loggedAt BETWEEN :start AND :end AND t.approvalStatus = 'APPROVED'")
    List<TimeLog> findApprovedByEmployeeUserIdAndLoggedAtBetween(
        @Param("employeeId") String employeeId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}