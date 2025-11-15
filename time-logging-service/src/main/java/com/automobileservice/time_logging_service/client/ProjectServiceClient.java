package com.automobileservice.time_logging_service.client;

import com.automobileservice.time_logging_service.client.dto.ProjectResponse;
import com.automobileservice.time_logging_service.client.dto.TaskResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "project-service")
public interface ProjectServiceClient {
    
    @GetMapping("/api/projects/{id}")
    ProjectResponse getProjectById(@PathVariable("id") UUID id);
    
    @GetMapping("/api/projects/employee/{employeeId}")
    List<ProjectResponse> getProjectsByEmployeeId(@PathVariable("employeeId") Long employeeId);
    
    @GetMapping("/api/projects/{projectId}/tasks")
    List<TaskResponse> getTasksByProjectId(@PathVariable("projectId") UUID projectId);
    
    @GetMapping("/api/tasks/by-assignee/{assigneeId}")
    List<TaskResponse> getTasksByAssigneeId(@PathVariable("assigneeId") Long assigneeId);
    
    @GetMapping("/api/projects/{projectId}/tasks/{taskId}")
    TaskResponse getTaskById(@PathVariable("projectId") UUID projectId, 
                            @PathVariable("taskId") UUID taskId);
}
