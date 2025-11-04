// ...existing code...
package com.autonova.progressmonitoring.controller;

import com.autonova.progressmonitoring.dto.ProjectMessageDto;
import com.autonova.progressmonitoring.mapper.ProjectMessageMapper;
import com.autonova.progressmonitoring.service.ProjectMessageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
public class ProjectMessageController {

    private final ProjectMessageService service;

    public ProjectMessageController(ProjectMessageService service) {
        this.service = service;
    }

    @GetMapping("/{projectId}/messages")
    public List<ProjectMessageDto> getMessages(@PathVariable String projectId) {
        UUID id = UUID.fromString(projectId);
        return service.getMessagesForProjectDto(id);
    }
}

