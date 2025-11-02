package com.autonova.customer.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CustomerResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        List<VehicleResponse> vehicles,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
