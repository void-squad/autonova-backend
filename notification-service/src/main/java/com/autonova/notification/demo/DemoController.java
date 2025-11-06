package com.autonova.notification.demo;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications/demo")
@ConditionalOnProperty(prefix = "app.demo", name = "enabled", havingValue = "true")
public class DemoController {

    private final DemoEventPublisher publisher;

    public DemoController(DemoEventPublisher publisher) {
        this.publisher = publisher;
    }

    @PostMapping("/appointment/{userId}")
    public ResponseEntity<Void> appointment(@PathVariable UUID userId) {
        publisher.publishAppointmentCreated(userId);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/project/{userId}")
    public ResponseEntity<Void> project(@PathVariable UUID userId) {
        publisher.publishProjectApproved(userId);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/payment/{userId}")
    public ResponseEntity<Void> payment(@PathVariable UUID userId) {
        publisher.publishPaymentSucceeded(userId);
        return ResponseEntity.accepted().build();
    }
}

