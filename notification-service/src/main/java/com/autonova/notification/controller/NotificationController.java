package com.autonova.notification.controller;

import com.autonova.notification.dto.NotificationDto;
import com.autonova.notification.sse.SseHub;
import com.autonova.notification.service.NotificationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;
    private final SseHub sseHub;

    public NotificationController(NotificationService notificationService, SseHub sseHub) {
        this.notificationService = notificationService;
        this.sseHub = sseHub;
    }

    @GetMapping(value = "/stream/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<NotificationDto>> stream(@PathVariable UUID userId) {
        return sseHub.subscribe(userId)
                .map(dto -> ServerSentEvent.<NotificationDto>builder()
                        .event(dto.type())
                        .id(dto.id() == null ? null : dto.id().toString())
                        .data(dto)
                        .build());
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<NotificationDto>> latest(@PathVariable UUID userId) {
        return ResponseEntity.ok(notificationService.latestForUser(userId));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable UUID id) {
        notificationService.markRead(id);
        return ResponseEntity.noContent().build();
    }
}

