package com.autonova.customer.dto;

import java.time.LocalDateTime;

public record VehicleResponse(
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
