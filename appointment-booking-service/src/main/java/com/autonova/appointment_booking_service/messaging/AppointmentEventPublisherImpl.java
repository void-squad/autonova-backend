package com.autonova.appointment_booking_service.messaging;

import com.autonova.appointment_booking_service.entity.Appointment;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
public class AppointmentEventPublisherImpl implements AppointmentEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final TopicExchange exchange;

    public AppointmentEventPublisherImpl(RabbitTemplate rabbitTemplate, TopicExchange exchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
    }

    @Override
    public void publish(String eventType, Appointment appointment) {
        if (eventType == null || eventType.isBlank() || appointment == null) return;

        Map<String, Object> root = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("appointment_id", safeStr(appointment.getId()));
        data.put("customer_id", safeStr(appointment.getCustomerId()));
        data.put("vehicle_id", safeStr(appointment.getVehicleId()));
        data.put("service_type", appointment.getServiceType());
        data.put("start_time", fmt(appointment.getStartTime()));
        data.put("end_time", fmt(appointment.getEndTime()));
        data.put("status", appointment.getStatus());
        if (appointment.getAssignedEmployeeId() != null) {
            data.put("assigned_employee_id", safeStr(appointment.getAssignedEmployeeId()));
        }
        root.put("data", data);

        Long numericUserId = uuidToBigint(appointment.getCustomerId());
        MessagePostProcessor mpp = msg -> {
            msg.getMessageProperties().setHeader("x-event-name", eventType);
            if (numericUserId != null) {
                msg.getMessageProperties().setHeader("x-recipients-user-ids", String.valueOf(numericUserId));
            }
            msg.getMessageProperties().setMessageId(safeStr(appointment.getId()) + ":" + eventType);
            return msg;
        };

        rabbitTemplate.convertAndSend(exchange.getName(), eventType, root, mpp);
    }

    private String fmt(java.time.OffsetDateTime dt) {
        return dt == null ? null : dt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private String safeStr(Object o) { return o == null ? null : String.valueOf(o); }

    // Convert UUID hex (leading zeros naturally removed) to a positive long within Long.MAX_VALUE
    private Long uuidToBigint(java.util.UUID u) {
        if (u == null) return null;
        String raw = u.toString().replace("-", ""); // remove dashes
        // Strip leading zeros
        String digits = raw.replaceFirst("^0+", "");
        if (digits.isEmpty()) return 0L;
        // If non-digit characters present (a-f), keep only numeric characters per requirement
        if (!digits.matches("\\d+")) {
            String only = digits.replaceAll("\\D+", "").replaceFirst("^0+", "");
            if (only.isEmpty()) return 0L;
            if (only.length() > 18) only = only.substring(0, 18); // prevent overflow
            return Long.parseLong(only);
        }
        if (digits.length() > 18) digits = digits.substring(0, 18); // prevent overflow
        return Long.parseLong(digits);
    }
}
