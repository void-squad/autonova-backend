package com.autonova.customer.dto;

/**
 * Public Vehicle Details DTO for cross-service consumption.
 * Exposes only essential fields needed by other services (e.g., Project Service).
 * 
 * Use Case: Project Service can fetch vehicle details to display:
 * "2020 Toyota Camry (ABC-123)"
 */
public record VehicleDetailsDto(
        Long id,
        String licensePlate,
        String make,
        String model,
        Integer year,
        String vin
) {
}
