package com.autonova.appointment_booking_service.messaging;

import com.autonova.appointment_booking_service.entity.Appointment;

public interface AppointmentEventPublisher {
    void publish(String eventType, Appointment appointment);
}

