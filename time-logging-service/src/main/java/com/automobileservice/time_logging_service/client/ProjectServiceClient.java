package com.automobileservice.time_logging_service.client;

import com.automobileservice.time_logging_service.client.dto.ProjectResponse;
import com.automobileservice.time_logging_service.client.dto.TaskResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "project-service")
public interface ProjectServiceClient {
    
    @GetMapping("/api/projects/{id}")
    ProjectResponse getProjectById(@PathVariable("id") String id);
    
    @GetMapping("/api/projects/{projectId}/tasks")
    List<TaskResponse> getTasksByProjectId(@PathVariable("projectId") String projectId);
    
    @GetMapping("/api/projects/{projectId}/tasks/{taskId}")
    TaskResponse getTaskById(@PathVariable("projectId") String projectId, 
                            @PathVariable("taskId") String taskId);
}
