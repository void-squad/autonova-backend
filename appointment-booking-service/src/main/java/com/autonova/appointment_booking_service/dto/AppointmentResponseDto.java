package com.autonova.appointment_booking_service.dto;

import lombok.*;
        import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponseDto {
    private UUID id;
    private UUID customerId;
    private String customerUsername;
    private UUID vehicleId;
    private String vehicleName;
    private String serviceType;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private String status;
    private UUID assignedEmployeeId;
    private String notes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
