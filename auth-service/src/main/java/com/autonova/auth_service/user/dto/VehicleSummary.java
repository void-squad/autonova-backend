package com.autonova.auth_service.user.dto;

import java.time.LocalDateTime;

/**
 * Representation of a customer's vehicle obtained from the customer-service.
 */
public record VehicleSummary(
        Long id,
        String make,
        String model,
        Integer year,
        String vin,
        String licensePlate,
        Long customerId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
