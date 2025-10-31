package com.automobileservice.time_logging_service.repository;

import com.automobileservice.time_logging_service.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, String> {
    
    // Find projects by status
    List<Project> findByStatus(String status);
    
    // Find projects assigned to an employee (via tasks)
    @Query("SELECT DISTINCT p FROM Project p JOIN ProjectTask pt ON pt.project.id = p.id " +
           "WHERE pt.assignedEmployee.userId = :employeeId")
    List<Project> findProjectsAssignedToEmployee(@Param("employeeId") String employeeId);
    
    // Find active projects (not completed or cancelled)
    @Query("SELECT p FROM Project p WHERE p.status NOT IN ('COMPLETED', 'CANCELLED')")
    List<Project> findActiveProjects();
    
    // Find projects by customer
    List<Project> findByCustomerUserId(String customerId);
}