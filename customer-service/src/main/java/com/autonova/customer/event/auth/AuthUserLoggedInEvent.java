package com.autonova.customer.event.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Message contract representing an authenticated user session emitted by
 * auth-service.
 */
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
