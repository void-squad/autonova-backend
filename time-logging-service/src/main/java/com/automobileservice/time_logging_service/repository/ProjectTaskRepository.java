package com.automobileservice.time_logging_service.repository;

import com.automobileservice.time_logging_service.entity.ProjectTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectTaskRepository extends JpaRepository<ProjectTask, String> {
    
    // Find tasks for a specific project
    List<ProjectTask> findByProjectId(String projectId);
    
    // Find tasks assigned to a specific employee
    List<ProjectTask> findByAssignedEmployeeUserId(String employeeId);
    
    // Find tasks by project and employee
    List<ProjectTask> findByProjectIdAndAssignedEmployeeUserId(
        String projectId, String employeeId);
    
    // Find tasks by status
    List<ProjectTask> findByStatus(String status);
    
    // Find incomplete tasks for an employee
    @Query("SELECT t FROM ProjectTask t WHERE t.assignedEmployee.userId = :employeeId " +
           "AND t.status NOT IN ('COMPLETED', 'CANCELLED')")
    List<ProjectTask> findIncompleteTasksByEmployee(@Param("employeeId") String employeeId);
}