package com.autonova.appointment_booking_service.controller;

import com.autonova.appointment_booking_service.dto.*;
import com.autonova.appointment_booking_service.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    public ResponseEntity<?> reschedule(@PathVariable("id") UUID id,
                                        @RequestParam("start") String startStr,
                                        @RequestParam("end") String endStr) {
        try {
            OffsetDateTime start = OffsetDateTime.parse(startStr);
            OffsetDateTime end = OffsetDateTime.parse(endStr);
            AppointmentResponseDto dto = service.reschedule(id, start, end);
            return ResponseEntity.ok(dto);
        } catch (java.time.format.DateTimeParseException ex) {
            return ResponseEntity.badRequest().body("Invalid date-time format for start/end. Use ISO_OFFSET_DATE_TIME, e.g. 2025-11-07T11:00:00+05:30");
        }
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

    @GetMapping("/availability/slots")
    public ResponseEntity<?> getAvailableSlots(
            @RequestParam("start") String startStr,
            @RequestParam("end") String endStr) {
        try {
            OffsetDateTime start = OffsetDateTime.parse(startStr);
            OffsetDateTime end = OffsetDateTime.parse(endStr);
            return ResponseEntity.ok(service.getAvailableSlots(start, end));
        } catch (java.time.format.DateTimeParseException ex) {
            return ResponseEntity.badRequest().body("Invalid date-time format. Use ISO_OFFSET_DATE_TIME, e.g. 2025-11-07T09:00:00+05:30");
        }
    }


    // ---------------- ADMIN ENDPOINTS ----------------

    @GetMapping("/all")
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AppointmentResponseDto>> getAllAppointments() {
        return ResponseEntity.ok(service.listAll());
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppointmentResponseDto> updateStatus(
            @PathVariable UUID id,
            @RequestParam String status) {
        return ResponseEntity.ok(service.updateStatus(id, status));
    }

//    @PutMapping("/{id}/assign")
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<AppointmentResponseDto> assignEmployee(
//            @PathVariable UUID id,
//            @RequestParam UUID employeeId) {
//        return ResponseEntity.ok(service.assignEmployee(id, employeeId));
//    }

    // Enhanced availability endpoint
//    @GetMapping("/availability/slots")
//    public ResponseEntity<?> getAvailableSlots(
//            @RequestParam("start") String startStr,
//            @RequestParam("end") String endStr) {
//        try {
//            OffsetDateTime start = OffsetDateTime.parse(startStr);
//            OffsetDateTime end = OffsetDateTime.parse(endStr);
//            return ResponseEntity.ok(service.getAvailableSlots(start, end));
//        } catch (java.time.format.DateTimeParseException ex) {
//            return ResponseEntity.badRequest().body("Invalid date-time format. Use ISO_OFFSET_DATE_TIME, e.g. 2025-11-07T09:00:00+05:30");
//        }
//    }
}