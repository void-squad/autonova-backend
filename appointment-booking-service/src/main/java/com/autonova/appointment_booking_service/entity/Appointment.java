package com.autonova.appointment_booking_service.entity;

import jakarta.persistence.*;
        import lombok.*;

        import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "appointments", indexes = {
        @Index(name = "idx_appt_customer", columnList = "customer_id"),
        @Index(name = "idx_appt_vehicle", columnList = "vehicle_id"),
        @Index(name = "idx_appt_employee", columnList = "assigned_employee_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;   // references customer microservice

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;    // references vehicle microservice

    @Column(name = "service_type", nullable = false)
    private String serviceType;

    @Column(name = "start_time", nullable = false)
    private OffsetDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private OffsetDateTime endTime;

    @Column(name = "status", nullable = false)
    private String status; // PENDING, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED

    @Column(name = "assigned_employee_id")
    private UUID assignedEmployeeId; // optional

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
