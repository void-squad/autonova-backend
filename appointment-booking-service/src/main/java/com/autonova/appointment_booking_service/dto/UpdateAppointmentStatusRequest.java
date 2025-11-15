package com.autonova.appointment_booking_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateAppointmentStatusRequest {

    @NotBlank(message = "status is required")
    private String status;

    private String adminNote;
}
