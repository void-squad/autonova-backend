package com.autonova.appointment_booking_service.controller;

import com.autonova.appointment_booking_service.dto.*;
import com.autonova.appointment_booking_service.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

        import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController {

    private final AppointmentService service;

    public AppointmentController(AppointmentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<AppointmentResponseDto> create(@Valid @RequestBody AppointmentRequestDto req) {
        AppointmentResponseDto dto = service.createAppointment(req);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{id}/reschedule")
    public ResponseEntity<AppointmentResponseDto> reschedule(@PathVariable("id") UUID id,
                                                             @RequestParam("start") OffsetDateTime start,
                                                             @RequestParam("end") OffsetDateTime end) {
        AppointmentResponseDto dto = service.reschedule(id, start, end);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable("id") UUID id,
                                       @RequestParam(value = "cancelledBy", required = false) UUID cancelledBy) {
        service.cancel(id, cancelledBy);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<AppointmentResponseDto>> listByCustomer(@PathVariable UUID customerId) {
        return ResponseEntity.ok(service.listByCustomer(customerId));
    }

    @GetMapping("/availability")
    public ResponseEntity<AvailabilityDto> checkAvailability(@RequestParam("start") OffsetDateTime start,
                                                             @RequestParam("end") OffsetDateTime end) {
        return ResponseEntity.ok(service.checkAvailability(start, end));
    }
}
