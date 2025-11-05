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
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
public class ProjectMessageController {

    private static final Logger log = LoggerFactory.getLogger(ProjectMessageController.class);

    private final ProjectMessageService service;
    private final EventPublisher publisher;

    public ProjectMessageController(ProjectMessageService service, EventPublisher publisher) {
        this.service = service;
        this.publisher = publisher;
    }

    @GetMapping("/{projectId}/messages")
    public ResponseEntity<List<ProjectMessageDto>> getMessages(@PathVariable String projectId) {
        UUID id;
        try {
            id = UUID.fromString(projectId);
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid projectId UUID: {}", projectId);
            return ResponseEntity.badRequest().build();
        }

        List<ProjectMessageDto> messages = service.getMessagesForProjectDto(id);
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/{projectId}/messages")
    public ResponseEntity<ProjectMessageDto> postStatusMessage(@PathVariable String projectId,
                                                               @RequestBody CreateStatusRequest request) {
        UUID id;
        try {
            id = UUID.fromString(projectId);
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid projectId UUID: {}", projectId);
            return ResponseEntity.badRequest().build();
        }

        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return ResponseEntity.badRequest().build();
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
            log.warn("Failed to publish status message for project {}", projectId, ex);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}

