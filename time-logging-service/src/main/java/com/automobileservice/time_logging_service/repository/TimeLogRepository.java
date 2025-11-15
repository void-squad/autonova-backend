package com.automobileservice.time_logging_service.repository;

import com.automobileservice.time_logging_service.entity.TimeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TimeLogRepository extends JpaRepository<TimeLog, UUID> {
    
    // Find all time logs ordered by most recent
    List<TimeLog> findAllByOrderByLoggedAtDesc();
    
    // Find all time logs for a specific employee, ordered by most recent
    List<TimeLog> findByEmployeeIdOrderByLoggedAtDesc(Long employeeId);
    
    // Find time logs for a specific project
    List<TimeLog> findByProjectIdOrderByLoggedAtDesc(UUID projectId);
    
    // Find time logs for a specific task
    List<TimeLog> findByTaskIdOrderByLoggedAtDesc(UUID taskId);
    
    // Calculate total hours logged by an employee
    @Query("SELECT COALESCE(SUM(t.hours), 0) FROM TimeLog t WHERE t.employeeId = :employeeId")
    BigDecimal getTotalHoursByEmployee(@Param("employeeId") Long employeeId);
    
    // Calculate total hours for a specific project
    @Query("SELECT COALESCE(SUM(t.hours), 0) FROM TimeLog t WHERE t.projectId = :projectId")
    BigDecimal getTotalHoursByProject(@Param("projectId") UUID projectId);
    
    // Calculate total hours for a specific task
    @Query("SELECT COALESCE(SUM(t.hours), 0) FROM TimeLog t WHERE t.taskId = :taskId")
    BigDecimal getTotalHoursByTask(@Param("taskId") UUID taskId);
    
    // Find time logs by employee and project
    List<TimeLog> findByEmployeeIdAndProjectIdOrderByLoggedAtDesc(
        Long employeeId, UUID projectId);
    
    // Find time logs after a specific date for an employee
    List<TimeLog> findByEmployeeIdAndLoggedAtAfter(Long employeeId, LocalDateTime after);
    
    // Find time logs between two dates for an employee
    List<TimeLog> findByEmployeeIdAndLoggedAtBetween(
        Long employeeId, LocalDateTime start, LocalDateTime end);
    
    // Find all pending time logs (for admin approval)
    List<TimeLog> findByApprovalStatusOrderByLoggedAtDesc(String approvalStatus);
    
    // Find pending time logs for a specific employee
    List<TimeLog> findByEmployeeIdAndApprovalStatusOrderByLoggedAtDesc(
        Long employeeId, String approvalStatus);
    
    // Calculate total hours by employee (approved only)
    @Query("SELECT COALESCE(SUM(t.hours), 0) FROM TimeLog t WHERE t.employeeId = :employeeId AND t.approvalStatus = 'APPROVED'")
    BigDecimal getTotalApprovedHoursByEmployee(@Param("employeeId") Long employeeId);
    
    // Calculate total hours for a task (approved only)
    @Query("SELECT COALESCE(SUM(t.hours), 0) FROM TimeLog t WHERE t.taskId = :taskId AND t.approvalStatus = 'APPROVED'")
    BigDecimal getTotalApprovedHoursByTask(@Param("taskId") UUID taskId);
    
    // Find approved time logs after a specific date for an employee
    @Query("SELECT t FROM TimeLog t WHERE t.employeeId = :employeeId AND t.loggedAt > :after AND t.approvalStatus = 'APPROVED'")
    List<TimeLog> findApprovedByEmployeeIdAndLoggedAtAfter(
        @Param("employeeId") Long employeeId, @Param("after") LocalDateTime after);
    
    // Find approved time logs between two dates for an employee
    @Query("SELECT t FROM TimeLog t WHERE t.employeeId = :employeeId AND t.loggedAt BETWEEN :start AND :end AND t.approvalStatus = 'APPROVED'")
    List<TimeLog> findApprovedByEmployeeIdAndLoggedAtBetween(
        @Param("employeeId") Long employeeId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}