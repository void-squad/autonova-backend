package com.autonova.appointment_booking_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

        import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentRequestDto {
    @NotNull
    private UUID customerId;
    @NotNull
    private UUID vehicleId;
    @NotNull
    private String serviceType;
    @NotNull
    private OffsetDateTime startTime;
    @NotNull
    private OffsetDateTime endTime;
    // optional: preferred employee
    private UUID preferredEmployeeId;
    private String notes;
}
