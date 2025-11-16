package com.automobileservice.time_logging_service.controller;

import com.automobileservice.time_logging_service.client.ProjectServiceClient;
import com.automobileservice.time_logging_service.dto.response.ProjectResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller to proxy project requests to the project-service.
 * Time-logging service doesn't store projects locally - it calls project-service via Feign.
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Slf4j
public class ProjectController {
    
    private final ProjectServiceClient projectServiceClient;
    
    /**
     * Get all projects assigned to an employee
     * This endpoint proxies the request to project-service
     * GET /api/projects/employee/{employeeId}
     */
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<ProjectResponse>> getProjectsForEmployee(@PathVariable Long employeeId) {
        log.info("REST request to get projects for employee: {}", employeeId);
        
        try {
            // Call project-service to get projects assigned to this employee
            List<com.automobileservice.time_logging_service.client.dto.ProjectResponse> projects = 
                projectServiceClient.getProjectsByEmployeeId(employeeId);
            
            // Map client DTOs to controller response DTOs
            List<ProjectResponse> response = projects.stream()
                .map(p -> ProjectResponse.builder()
                    .id(p.getProjectId().toString())
                    .title(p.getTitle())
                    .description(p.getDescription())
                    .status(p.getStatus())
                    .build())
                .toList();
            
            log.info("Found {} projects for employee {}", response.size(), employeeId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching projects for employee {}: {}", employeeId, e.getMessage());
            // Return empty list if project-service is down or employee has no projects
            return ResponseEntity.ok(List.of());
        }
    }
    
    /**
     * Get project by ID from project-service
     * GET /api/projects/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<com.automobileservice.time_logging_service.client.dto.ProjectResponse> getProjectById(@PathVariable UUID id) {
        log.info("REST request to get project: {}", id);
        com.automobileservice.time_logging_service.client.dto.ProjectResponse project = projectServiceClient.getProjectById(id);
        return ResponseEntity.ok(project);
    }
}