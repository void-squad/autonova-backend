package com.autonova.customer.event;

import com.autonova.customer.model.Vehicle;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public record VehicleEvent(
        UUID eventId,
        VehicleEventType type,
        Long vehicleId,
        Long customerId,
        String vin,
        String licensePlate,
        String make,
        String model,
        Integer year,
        OffsetDateTime occurredAt
) {

    public static VehicleEvent from(VehicleEventType type, Vehicle vehicle) {
        Long customerId = vehicle.getCustomer() != null ? vehicle.getCustomer().getId() : null;
        return new VehicleEvent(
                UUID.randomUUID(),
                type,
                vehicle.getId(),
                customerId,
                vehicle.getVin(),
                vehicle.getLicensePlate(),
                vehicle.getMake(),
                vehicle.getModel(),
                vehicle.getYear(),
                OffsetDateTime.now(ZoneOffset.UTC)
        );
    }
}
