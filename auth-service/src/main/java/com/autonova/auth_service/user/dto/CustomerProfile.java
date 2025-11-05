package com.autonova.auth_service.user.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Representation of the customer profile returned by customer-service.
 */
public record CustomerProfile(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        List<VehicleSummary> vehicles,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
