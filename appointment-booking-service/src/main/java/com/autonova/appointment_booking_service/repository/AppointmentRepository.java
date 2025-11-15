package com.autonova.appointment_booking_service.repository;

import com.autonova.appointment_booking_service.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    // appointments for a customer
    List<Appointment> findByCustomerIdOrderByStartTimeDesc(UUID customerId);

    // overlapping detection for an employee
    @Query("SELECT a FROM Appointment a WHERE a.assignedEmployeeId = :employeeId " +
            "AND a.status <> 'CANCELLED' AND (a.startTime < :end AND a.endTime > :start)")
    List<Appointment> findOverlappingForEmployee(@Param("employeeId") UUID employeeId,
                                                 @Param("start") OffsetDateTime start,
                                                 @Param("end") OffsetDateTime end);

    // overlapping detection for a vehicle
    @Query("SELECT a FROM Appointment a WHERE a.vehicleId = :vehicleId " +
            "AND a.status <> 'CANCELLED' AND (a.startTime < :end AND a.endTime > :start)")
    List<Appointment> findOverlappingForVehicle(@Param("vehicleId") UUID vehicleId,
                                                @Param("start") OffsetDateTime start,
                                                @Param("end") OffsetDateTime end);

    // find appointments in a given range (for availability)
    @Query("SELECT a FROM Appointment a WHERE a.startTime < :end AND a.endTime > :start AND a.status <> 'CANCELLED'")
    List<Appointment> findInTimeRange(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);

    @Query("""
            SELECT a FROM Appointment a
            WHERE (:status IS NULL OR UPPER(a.status) = UPPER(:status))
              AND (:from IS NULL OR a.startTime >= :from)
              AND (:to IS NULL OR a.startTime <= :to)
            ORDER BY a.startTime DESC
            """)
    List<Appointment> searchForAdmin(@Param("status") String status,
                                     @Param("from") OffsetDateTime from,
                                     @Param("to") OffsetDateTime to);
}
