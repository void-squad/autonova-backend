package com.autonova.progressmonitoring.controller;

import com.autonova.progressmonitoring.sse.SseEmitterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/sse")
public class SseController {
    private static final Logger log = LoggerFactory.getLogger(SseController.class);

    private final SseEmitterRegistry registry;

    public SseController(SseEmitterRegistry registry) {
        this.registry = registry;
    }

    @GetMapping(path = "/projects/{projectId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'CUSTOMER')")
    public SseEmitter subscribeToProject(@PathVariable String projectId) {
        try {
            UUID.fromString(projectId);
        } catch (Exception ex) {
            throw new IllegalArgumentException("projectId must be a GUID");
        }

        SseEmitter emitter = new SseEmitter(0L);
        registry.register(projectId, emitter);

        emitter.onCompletion(() -> {
            log.debug("SSE completed for project {}", projectId);
            registry.remove(projectId, emitter);
        });

        emitter.onTimeout(() -> {
            log.debug("SSE timeout for project {}", projectId);
            registry.remove(projectId, emitter);
        });

        emitter.onError((ex) -> {
            log.debug("SSE error for project {}: {}", projectId, ex.getMessage());
            registry.remove(projectId, emitter);
        });

        try {
            emitter.send(SseEmitter.event().name("connected").data("subscribed"));
        } catch (Exception ex) {
            log.warn("Failed to send initial connected event to SSE client for project {}", projectId, ex);
            // if sending initial event fails, remove emitter to avoid leaking
            registry.remove(projectId, emitter);
        }

        return emitter;
    }
}
