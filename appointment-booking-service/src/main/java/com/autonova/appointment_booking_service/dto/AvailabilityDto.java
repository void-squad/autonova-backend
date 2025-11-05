package com.autonova.appointment_booking_service.dto;

import lombok.Data;
import java.time.OffsetDateTime;
import java.util.List;

@Data
public class AvailabilityDto {
    private OffsetDateTime start;
    private OffsetDateTime end;
    private boolean available;
    private List<String> reasons; // eg. overlapping appointments
}
