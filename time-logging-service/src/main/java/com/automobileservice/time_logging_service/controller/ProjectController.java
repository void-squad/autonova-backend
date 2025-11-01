package com.automobileservice.time_logging_service.controller;

import com.automobileservice.time_logging_service.dto.response.ProjectResponse;
import com.automobileservice.time_logging_service.entity.Project;
import com.automobileservice.time_logging_service.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ProjectController {
    
    private final ProjectRepository projectRepository;
    
    /**
     * Get all projects assigned to an employee
     * GET /api/projects/employee/{employeeId}
     */
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<ProjectResponse>> getProjectsForEmployee(@PathVariable String employeeId) {
        log.info("REST request to get projects for employee: {}", employeeId);
        List<Project> projects = projectRepository.findProjectsAssignedToEmployee(employeeId);
        List<ProjectResponse> response = projects.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get all active projects
     * GET /api/projects/active
     */
    @GetMapping("/active")
    public ResponseEntity<List<ProjectResponse>> getActiveProjects() {
        log.info("REST request to get active projects");
        List<Project> projects = projectRepository.findActiveProjects();
        List<ProjectResponse> response = projects.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get project by ID
     * GET /api/projects/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable String id) {
        log.info("REST request to get project: {}", id);
        Project project = projectRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Project not found: " + id));
        return ResponseEntity.ok(mapToResponse(project));
    }
    
    // Helper method to map entity to response
    private ProjectResponse mapToResponse(Project project) {
        return ProjectResponse.builder()
            .id(project.getId())
            .customerId(project.getCustomer().getUserId())
            .customerName(project.getCustomer().getUser().getFirstName() + " " + 
                         project.getCustomer().getUser().getLastName())
            .vehicleId(project.getVehicle().getId())
            .vehicleInfo(project.getVehicle().getYear() + " " + 
                        project.getVehicle().getMake() + " " + 
                        project.getVehicle().getModel())
            .projectType(project.getProjectType())
            .title(project.getTitle())
            .description(project.getDescription())
            .status(project.getStatus())
            .priority(project.getPriority())
            .estimatedCost(project.getEstimatedCost())
            .startDate(project.getStartDate())
            .endDate(project.getEndDate())
            .build();
    }
}