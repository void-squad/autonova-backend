package com.automobileservice.time_logging_service.controller;

import com.automobileservice.time_logging_service.dto.response.TaskResponse;
import com.automobileservice.time_logging_service.entity.ProjectTask;
import com.automobileservice.time_logging_service.repository.ProjectTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class TaskController {
    
    private final ProjectTaskRepository taskRepository;
    
    /**
     * Get all tasks for a specific project
     * GET /api/tasks/project/{projectId}
     */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<TaskResponse>> getTasksByProject(@PathVariable String projectId) {
        log.info("REST request to get tasks for project: {}", projectId);
        List<ProjectTask> tasks = taskRepository.findByProjectId(projectId);
        List<TaskResponse> response = tasks.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get all tasks assigned to an employee
     * GET /api/tasks/employee/{employeeId}
     */
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<TaskResponse>> getTasksByEmployee(@PathVariable String employeeId) {
        log.info("REST request to get tasks for employee: {}", employeeId);
        List<ProjectTask> tasks = taskRepository.findByAssignedEmployeeUserId(employeeId);
        List<TaskResponse> response = tasks.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get incomplete tasks for an employee
     * GET /api/tasks/employee/{employeeId}/incomplete
     */
    @GetMapping("/employee/{employeeId}/incomplete")
    public ResponseEntity<List<TaskResponse>> getIncompleteTasksByEmployee(@PathVariable String employeeId) {
        log.info("REST request to get incomplete tasks for employee: {}", employeeId);
        List<ProjectTask> tasks = taskRepository.findIncompleteTasksByEmployee(employeeId);
        List<TaskResponse> response = tasks.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get tasks for a project assigned to an employee
     * GET /api/tasks/project/{projectId}/employee/{employeeId}
     */
    @GetMapping("/project/{projectId}/employee/{employeeId}")
    public ResponseEntity<List<TaskResponse>> getTasksByProjectAndEmployee(
            @PathVariable String projectId,
            @PathVariable String employeeId) {
        log.info("REST request to get tasks for project: {} and employee: {}", projectId, employeeId);
        List<ProjectTask> tasks = taskRepository.findByProjectIdAndAssignedEmployeeUserId(projectId, employeeId);
        List<TaskResponse> response = tasks.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    // Helper method to map entity to response
    private TaskResponse mapToResponse(ProjectTask task) {
        return TaskResponse.builder()
            .id(task.getId())
            .projectId(task.getProject().getId())
            .taskName(task.getTaskName())
            .description(task.getDescription())
            .assignedEmployeeId(task.getAssignedEmployee() != null ? 
                task.getAssignedEmployee().getUserId() : null)
            .assignedEmployeeName(task.getAssignedEmployee() != null ? 
                task.getAssignedEmployee().getUser().getFirstName() + " " + 
                task.getAssignedEmployee().getUser().getLastName() : null)
            .estimatedHours(task.getEstimatedHours())
            .actualHours(task.getActualHours())
            .status(task.getStatus())
            .priority(task.getPriority())
            .dueDate(task.getDueDate())
            .build();
    }
}