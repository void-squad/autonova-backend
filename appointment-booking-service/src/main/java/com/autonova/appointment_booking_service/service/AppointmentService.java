package com.autonova.appointment_booking_service.service;

import com.autonova.appointment_booking_service.dto.*;
import com.autonova.appointment_booking_service.entity.Appointment;
import com.autonova.appointment_booking_service.exception.ConflictException;
import com.autonova.appointment_booking_service.repository.AppointmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class AppointmentService {

    private final AppointmentRepository repository;

    // capacity: number of concurrent service bays; configurable in production
    private final int serviceCapacity = 3;

    private static final Set<String> VALID_STATUSES = Set.of(
            "PENDING",
            "CONFIRMED",
            "IN_PROGRESS",
            "COMPLETED",
            "CANCELLED"
    );

    public AppointmentService(AppointmentRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public AppointmentResponseDto createAppointment(AppointmentRequestDto req) {
        // Basic validation
        if (req.getEndTime().isBefore(req.getStartTime()) || req.getEndTime().equals(req.getStartTime())) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }

        // Conflict: vehicle overlapping
        List<Appointment> vehicleOverlaps = repository.findOverlappingForVehicle(
                req.getVehicleId(), req.getStartTime(), req.getEndTime());
        if (!vehicleOverlaps.isEmpty()) {
            throw new ConflictException("Vehicle already has an appointment in the requested slot");
        }

        // If preferred employee provided, check employee's schedule
        if (req.getPreferredEmployeeId() != null) {
            List<Appointment> empOverlaps = repository.findOverlappingForEmployee(
                    req.getPreferredEmployeeId(), req.getStartTime(), req.getEndTime());
            if (!empOverlaps.isEmpty()) {
                throw new ConflictException("Preferred employee is unavailable in that slot");
            }
        }

        // Capacity check: count overlapping appointments (not cancelled)
        List<Appointment> overlaps = repository.findInTimeRange(req.getStartTime(), req.getEndTime());
        if (overlaps.size() >= serviceCapacity) {
            throw new ConflictException("No available service capacity in the requested slot");
        }

        Appointment a = Appointment.builder()
                .id(UUID.randomUUID())
                .customerId(req.getCustomerId())
                .customerUsername(req.getCustomerUsername())
                .vehicleId(req.getVehicleId())
                .vehicleName(req.getVehicleName())
                .serviceType(req.getServiceType())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .status("PENDING")
                .assignedEmployeeId(req.getPreferredEmployeeId())
                .notes(req.getNotes())
                .createdAt(OffsetDateTime.now())
                .build();

        Appointment saved = repository.save(a);
        return toDto(saved);
    }

    @Transactional
    public AppointmentResponseDto reschedule(UUID appointmentId, OffsetDateTime newStart, OffsetDateTime newEnd) {
        Appointment appt = repository.findById(appointmentId)
                .orElseThrow(() -> new NoSuchElementException("Appointment not found"));

        if ("CANCELLED".equals(appt.getStatus())) {
            throw new IllegalStateException("Cannot reschedule a cancelled appointment");
        }

        // conflict checks similar to create
        List<Appointment> vehicleOverlaps = repository.findOverlappingForVehicle(
                appt.getVehicleId(), newStart, newEnd);
        // remove self from overlaps if present
        vehicleOverlaps.removeIf(a -> a.getId().equals(appointmentId));
        if (!vehicleOverlaps.isEmpty()) {
            throw new ConflictException("Vehicle has another appointment in that slot");
        }

        if (appt.getAssignedEmployeeId() != null) {
            List<Appointment> empOverlaps = repository.findOverlappingForEmployee(
                    appt.getAssignedEmployeeId(), newStart, newEnd);
            empOverlaps.removeIf(a -> a.getId().equals(appointmentId));
            if (!empOverlaps.isEmpty()) {
                throw new ConflictException("Assigned employee unavailable in that slot");
            }
        }

        List<Appointment> overlaps = repository.findInTimeRange(newStart, newEnd);
        overlaps.removeIf(a -> a.getId().equals(appointmentId));
        if (overlaps.size() >= serviceCapacity) {
            throw new ConflictException("No capacity in the requested slot");
        }

        appt.setStartTime(newStart);
        appt.setEndTime(newEnd);
        appt.setUpdatedAt(OffsetDateTime.now());
        Appointment saved = repository.save(appt);
        return toDto(saved);
    }

    @Transactional
    public void cancel(UUID appointmentId, UUID cancelledBy) {
        Appointment appt = repository.findById(appointmentId)
                .orElseThrow(() -> new NoSuchElementException("Appointment not found"));
        appt.setStatus("CANCELLED");
        appt.setUpdatedAt(OffsetDateTime.now());
        repository.save(appt);
        // publish cancellation event to notification service / audit
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponseDto> listByCustomer(UUID customerId) {
        return repository.findByCustomerIdOrderByStartTimeDesc(customerId)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, OffsetDateTime>> getAvailableSlots(OffsetDateTime start, OffsetDateTime end) {
        List<Map<String, OffsetDateTime>> availableSlots = new ArrayList<>();

        OffsetDateTime current = start;
        while (current.isBefore(end)) {
            OffsetDateTime slotStart = current;
            OffsetDateTime slotEnd = current.plusHours(1);
            if (slotEnd.isAfter(end)) break;

            List<Appointment> conflicts = repository.findInTimeRange(slotStart, slotEnd);
            if (conflicts.isEmpty())
            {
                availableSlots.add(Map.of("start", slotStart, "end", slotEnd));
            }

            current = slotEnd;
        }

        return availableSlots;
    }


    private AppointmentResponseDto toDto(Appointment a) {
        return new AppointmentResponseDto(
                a.getId(),
                a.getCustomerId(),
                a.getCustomerUsername(),
                a.getVehicleId(),
                a.getVehicleName(),
                a.getServiceType(),
                a.getStartTime(),
                a.getEndTime(),
                a.getStatus(),
                a.getAssignedEmployeeId(),
                a.getNotes(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }

    // ---------------- ADMIN FUNCTIONS ----------------

    @Transactional(readOnly = true)
    public List<AppointmentResponseDto> listAll() {
        return repository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public AppointmentResponseDto updateStatus(UUID appointmentId, String newStatus) {
        Appointment appt = repository.findById(appointmentId)
                .orElseThrow(() -> new NoSuchElementException("Appointment not found"));
        appt.setStatus(newStatus.toUpperCase(Locale.ROOT));
        appt.setUpdatedAt(OffsetDateTime.now());
        Appointment saved = repository.save(appt);
        return toDto(saved);
    }

    @Transactional
    public AppointmentResponseDto assignEmployee(UUID appointmentId, UUID employeeId) {
        Appointment appt = repository.findById(appointmentId)
                .orElseThrow(() -> new NoSuchElementException("Appointment not found"));
        appt.setAssignedEmployeeId(employeeId);
        appt.setUpdatedAt(OffsetDateTime.now());
        Appointment saved = repository.save(appt);
        return toDto(saved);
    }

// ---------------- ENHANCED AVAILABILITY ----------------

//    @Transactional(readOnly = true)
//    public List<Map<String, OffsetDateTime>> getAvailableSlots(OffsetDateTime start, OffsetDateTime end) {
//        List<Map<String, OffsetDateTime>> availableSlots = new ArrayList<>();
//
//        // Generate 1-hour time windows between start and end
//        OffsetDateTime current = start;
//        while (current.isBefore(end)) {
//            OffsetDateTime slotStart = current;
//            OffsetDateTime slotEnd = current.plusHours(1);
//            if (slotEnd.isAfter(end)) break;
//
//            // Check if this 1-hour slot is free
//            List<Appointment> conflicts = repository.findInTimeRange(slotStart, slotEnd);
//            if (conflicts.size() < serviceCapacity) {
//                availableSlots.add(Map.of(
//                        "start", slotStart,
//                        "end", slotEnd
//                ));
//            }
//
//            current = slotEnd; // move to next hour
//        }
//
//        return availableSlots;
//    }
}