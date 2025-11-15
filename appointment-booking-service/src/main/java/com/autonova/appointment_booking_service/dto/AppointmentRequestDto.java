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
    private String customerUsername;
    @NotNull
    private UUID vehicleId;
    private String vehicleName;
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
