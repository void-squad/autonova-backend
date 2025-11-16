package com.autonova.progressmonitoring.controller;

import com.autonova.progressmonitoring.dto.CreateStatusRequest;
import com.autonova.progressmonitoring.dto.ProjectMessageDto;
import com.autonova.progressmonitoring.messaging.publisher.EventPublisher;
import com.autonova.progressmonitoring.service.ProjectMessageService;
import com.autonova.progressmonitoring.factory.ProjectMessageFactory;
import com.autonova.progressmonitoring.storage.AttachmentStorage;
import com.autonova.progressmonitoring.storage.StoredAttachment;
import com.autonova.progressmonitoring.service.ProjectClientService;
import com.autonova.progressmonitoring.client.ProjectServiceClient.ProjectServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/projects")
public class ProjectMessageController {

    private static final Logger log = LoggerFactory.getLogger(ProjectMessageController.class);
    private final ProjectMessageService service;
    private final EventPublisher publisher;
    private final AttachmentStorage attachmentStorage;
    private final ProjectClientService projectClientService;
    private final ObjectMapper objectMapper;

    public ProjectMessageController(ProjectMessageService service, EventPublisher publisher, AttachmentStorage attachmentStorage, ProjectClientService projectClientService, ObjectMapper objectMapper) {
        this.service = service;
        this.publisher = publisher;
        this.attachmentStorage = attachmentStorage;
        this.projectClientService = projectClientService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/my/statuses")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<Map<String, Object>>> getMyProjectStatuses(HttpServletRequest request) {
        Object uid = request.getAttribute("userId");
        if (uid == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        long userId;
        try {
            userId = (Long) uid;
        } catch (ClassCastException ex) {
            // sometimes numbers deserialized differently; try to convert
            try {
                userId = Long.parseLong(String.valueOf(uid));
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }
        }

        try {
            List<Map<String, Object>> projects = projectClientService.getProjectsForCustomer(userId);
            List<Map<String, Object>> response = new ArrayList<>();

            if (projects == null || projects.isEmpty()) {
                return ResponseEntity.ok(response);
            }

            for (Map<String, Object> proj : projects) {
                Map<String, Object> item = new HashMap<>();
                item.put("project", proj);

                Object projIdObj = proj.getOrDefault("projectId", proj.get("projectId"));
                if (projIdObj != null) {
                    try {
                        UUID pid = UUID.fromString(String.valueOf(projIdObj));
                        List<ProjectMessageDto> messages = service.getMessagesForProjectDto(pid);
                        if (messages != null && !messages.isEmpty()) {
                            ProjectMessageDto last = messages.get(0);
                            Map<String, Object> lastMsg = new HashMap<>();
                            lastMsg.put("message", last.getMessage());
                            lastMsg.put("occurredAt", last.getOccurredAt());
                            lastMsg.put("category", last.getCategory());
                            item.put("lastMessage", lastMsg);
                        }
                    } catch (IllegalArgumentException ex) {
                        // ignore invalid projectId format
                    }
                }

                response.add(item);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching projects for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/{projectId}/messages")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'CUSTOMER')")
    public ResponseEntity<List<ProjectMessageDto>> getMessages(@PathVariable String projectId) {
        UUID id;
        try {
            id = UUID.fromString(projectId);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(null); // Invalid UUID format
        }

        try {
            List<ProjectMessageDto> messages = service.getMessagesForProjectDto(id);
            projectClientService.enrichMessagesWithProjectTitle(projectId, messages);
            return ResponseEntity.ok(messages);
        } catch (ProjectServiceException e) {
            log.error("Error fetching project data for project {}: {}", projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null); // Internal error from Project Service Client
        } catch (Exception e) {
            log.error("Unexpected error occurred while fetching messages for project {}: {}", projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null); // Generic error handling
        }
    }

    @GetMapping("/{projectId}/messages/page")
    public ResponseEntity<Slice<ProjectMessageDto>> getMessagesPage(@PathVariable String projectId,
                                                                    @RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "20") int size) {
        UUID id;
        try {
            id = UUID.fromString(projectId);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(null);
        }

        try {
            Slice<ProjectMessageDto> slice = service.getMessagesPage(id, page, size);
            return ResponseEntity.ok(slice);
        } catch (Exception e) {
            log.error("Error fetching paginated messages for project {}: {}", projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/{projectId}/messages/before")
    public ResponseEntity<Slice<ProjectMessageDto>> getMessagesBefore(@PathVariable String projectId,
                                                                      @RequestParam OffsetDateTime before,
                                                                      @RequestParam(defaultValue = "20") int size) {
        UUID id;
        try {
            id = UUID.fromString(projectId);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(null);
        }

        try {
            Slice<ProjectMessageDto> slice = service.getMessagesBefore(id, before, size);
            return ResponseEntity.ok(slice);
        } catch (Exception e) {
            log.error("Error fetching messages before timestamp for project {}: {}", projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/{projectId}/messages/after")
    public ResponseEntity<Slice<ProjectMessageDto>> getMessagesAfter(@PathVariable String projectId,
                                                                     @RequestParam OffsetDateTime after,
                                                                     @RequestParam(defaultValue = "20") int size) {
        UUID id;
        try {
            id = UUID.fromString(projectId);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(null);
        }

        try {
            Slice<ProjectMessageDto> slice = service.getMessagesAfter(id, after, size);
            return ResponseEntity.ok(slice);
        } catch (Exception e) {
            log.error("Error fetching messages after timestamp for project {}: {}", projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/{projectId}/messages")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ProjectMessageDto> postStatusMessage(@PathVariable String projectId,
                                                               @RequestBody CreateStatusRequest request) {
        UUID id;
        try {
            id = UUID.fromString(projectId);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return ResponseEntity.badRequest().build(); // Bad request if message is missing
        }

        try {
            ProjectMessageDto saved = service.saveMessage(
                    ProjectMessageFactory.fromManual(id,
                            request.getCategory(),
                            request.getMessage(),
                            request.getPayload(),
                            request.getOccurredAt(),
                            request.getAttachmentUrl(),
                            request.getAttachmentContentType(),
                            request.getAttachmentFilename(),
                            request.getAttachmentSize())
            );

            try {
                String jsonPayload = objectMapper.writeValueAsString(saved);
                log.debug("Publishing SSE message to project {}: {}", projectId, jsonPayload);
                publisher.publishToProject(projectId, jsonPayload);
                log.info("Successfully published SSE message for project {}", projectId);
            } catch (Exception ex) {
                log.error("Failed to publish status message for project {}: {}", projectId, ex.getMessage(), ex);
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            log.error("Error saving message for project {}: {}", projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/{projectId}/messages/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ProjectMessageDto> uploadAndCreateMessage(@PathVariable String projectId,
                                                                    @RequestPart("file") MultipartFile file,
                                                                    @RequestPart("message") String message,
                                                                    @RequestPart(value = "category", required = false) String category) {
        UUID id;
        try {
            id = UUID.fromString(projectId);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }

        if (file.isEmpty() || message == null || message.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            StoredAttachment att = attachmentStorage.store(file);
            ProjectMessageDto saved = service.saveMessage(ProjectMessageFactory.fromManualWithAttachment(id, category, message, att));

            try {
                String jsonPayload = objectMapper.writeValueAsString(saved);
                log.debug("Publishing SSE message with attachment to project {}: {}", projectId, jsonPayload);
                publisher.publishToProject(projectId, jsonPayload);
                log.info("Successfully published SSE message with attachment for project {}", projectId);
            } catch (Exception ex) {
                log.error("Failed to publish status message with attachment for project {}: {}", projectId, ex.getMessage(), ex);
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            log.error("Error uploading file and saving message for project {}: {}", projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/{projectId}/messages/test")
    public String test() {
        return "Test";
    }

}
