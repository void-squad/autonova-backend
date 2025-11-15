package com.autonova.progressmonitoring.controller;

import com.autonova.progressmonitoring.dto.CreateStatusRequest;
import com.autonova.progressmonitoring.dto.ProjectMessageDto;
import com.autonova.progressmonitoring.messaging.publisher.EventPublisher;
import com.autonova.progressmonitoring.service.ProjectMessageService;
import com.autonova.progressmonitoring.factory.ProjectMessageFactory;
import com.autonova.progressmonitoring.storage.AttachmentStorage;
import com.autonova.progressmonitoring.storage.StoredAttachment;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
public class ProjectMessageController {

    private static final Logger log = LoggerFactory.getLogger(ProjectMessageController.class);
    private final ProjectMessageService service;
    private final EventPublisher publisher;
    private final AttachmentStorage attachmentStorage;

    public ProjectMessageController(ProjectMessageService service, EventPublisher publisher, AttachmentStorage attachmentStorage) {
        this.service = service;
        this.publisher = publisher;
        this.attachmentStorage = attachmentStorage;
    }

    @GetMapping("/{projectId}/messages")
    public ResponseEntity<List<ProjectMessageDto>> getMessages(@PathVariable String projectId) {
        UUID id;
        try { id = UUID.fromString(projectId); } catch (IllegalArgumentException ex) { return ResponseEntity.badRequest().build(); }
        List<ProjectMessageDto> messages = service.getMessagesForProjectDto(id);
        return ResponseEntity.ok(messages);
    }

    // New paginated timeline endpoint (page index, size) newest first
    @GetMapping("/{projectId}/messages/page")
    public ResponseEntity<Slice<ProjectMessageDto>> getMessagesPage(@PathVariable String projectId,
                                                                    @RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "20") int size) {
        UUID id;
        try { id = UUID.fromString(projectId); } catch (IllegalArgumentException ex) { return ResponseEntity.badRequest().build(); }
        Slice<ProjectMessageDto> slice = service.getMessagesPage(id, page, size);
        return ResponseEntity.ok(slice);
    }

    // Cursor-style: older than createdAt timestamp
    @GetMapping("/{projectId}/messages/before")
    public ResponseEntity<Slice<ProjectMessageDto>> getMessagesBefore(@PathVariable String projectId,
                                                                      @RequestParam OffsetDateTime before,
                                                                      @RequestParam(defaultValue = "20") int size) {
        UUID id;
        try { id = UUID.fromString(projectId); } catch (IllegalArgumentException ex) { return ResponseEntity.badRequest().build(); }
        Slice<ProjectMessageDto> slice = service.getMessagesBefore(id, before, size);
        return ResponseEntity.ok(slice);
    }

    // Cursor-style: newer than createdAt timestamp (ascending order)
    @GetMapping("/{projectId}/messages/after")
    public ResponseEntity<Slice<ProjectMessageDto>> getMessagesAfter(@PathVariable String projectId,
                                                                     @RequestParam OffsetDateTime after,
                                                                     @RequestParam(defaultValue = "20") int size) {
        UUID id;
        try { id = UUID.fromString(projectId); } catch (IllegalArgumentException ex) { return ResponseEntity.badRequest().build(); }
        Slice<ProjectMessageDto> slice = service.getMessagesAfter(id, after, size);
        return ResponseEntity.ok(slice);
    }

    @PostMapping("/{projectId}/messages")
    public ResponseEntity<ProjectMessageDto> postStatusMessage(@PathVariable String projectId,
                                                               @RequestBody CreateStatusRequest request) {
        UUID id;
        try { id = UUID.fromString(projectId); } catch (IllegalArgumentException ex) { return ResponseEntity.badRequest().build(); }
        if (request.getMessage() == null || request.getMessage().isBlank()) { return ResponseEntity.badRequest().build(); }

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

        try { publisher.publishMessageToProject(projectId, saved.getMessage()); } catch (Exception ex) { log.warn("Failed to publish status message for project {}", projectId, ex); }
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // Multipart upload endpoint for manual progress image
    @PostMapping(value = "/{projectId}/messages/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProjectMessageDto> uploadAndCreateMessage(@PathVariable String projectId,
                                                                    @RequestPart("file") MultipartFile file,
                                                                    @RequestPart("message") String message,
                                                                    @RequestPart(value = "category", required = false) String category) {
        UUID id;
        try { id = UUID.fromString(projectId); } catch (IllegalArgumentException ex) { return ResponseEntity.badRequest().build(); }
        if (file.isEmpty()) return ResponseEntity.badRequest().build();
        if (message == null || message.isBlank()) return ResponseEntity.badRequest().build();

        StoredAttachment att = attachmentStorage.store(file);
        ProjectMessageDto saved = service.saveMessage(ProjectMessageFactory.fromManualWithAttachment(id, category, message, att));

        try { publisher.publishMessageToProject(projectId, saved.getMessage()); } catch (Exception ex) { log.warn("Failed to publish status message for project {}", projectId, ex); }
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
