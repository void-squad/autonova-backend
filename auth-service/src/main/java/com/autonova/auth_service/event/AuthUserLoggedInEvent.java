package com.autonova.auth_service.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthUserLoggedInEvent(
        UUID eventId,
        UUID userId,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        Set<String> roles,
        Instant occurredAt) {
}
