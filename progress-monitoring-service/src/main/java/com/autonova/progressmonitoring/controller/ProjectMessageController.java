package com.autonova.progressmonitoring.controller;

import com.autonova.progressmonitoring.dto.CreateStatusRequest;
import com.autonova.progressmonitoring.dto.ProjectMessageDto;
import com.autonova.progressmonitoring.messaging.publisher.EventPublisher;
import com.autonova.progressmonitoring.service.ProjectMessageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
public class ProjectMessageController {

    private final ProjectMessageService service;
    private final EventPublisher publisher;

    public ProjectMessageController(ProjectMessageService service, EventPublisher publisher) {
        this.service = service;
        this.publisher = publisher;
    }

    @GetMapping("/{projectId}/messages")
    public List<ProjectMessageDto> getMessages(@PathVariable String projectId) {
        UUID id = UUID.fromString(projectId);
        return service.getMessagesForProjectDto(id);
    }

    @PostMapping("/{projectId}/messages")
    public org.springframework.http.ResponseEntity<ProjectMessageDto> postStatusMessage(@PathVariable String projectId,
                                                                                       @org.springframework.web.bind.annotation.RequestBody CreateStatusRequest request) {
        UUID id = UUID.fromString(projectId);

        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return org.springframework.http.ResponseEntity.badRequest().build();
        }

        ProjectMessageDto saved = service.saveMessage(id,
                request.getCategory(),
                request.getMessage(),
                request.getPayload(),
                request.getOccurredAt());

        try {
            // notify subscribers about the human-friendly status update
            publisher.publishMessageToProject(projectId, saved.getMessage());
        } catch (Exception ex) {
            // don't fail the request if publishing fails; log and return created
            org.slf4j.LoggerFactory.getLogger(ProjectMessageController.class).warn("Failed to publish status message for project {}", projectId, ex);
        }

        return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(saved);
    }
}

