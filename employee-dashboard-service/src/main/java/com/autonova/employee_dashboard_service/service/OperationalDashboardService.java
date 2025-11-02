package com.autonova.employee_dashboard_service.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.autonova.employee_dashboard_service.dto.OperationalViewResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationalDashboardService {

    private final WebClient.Builder webClientBuilder;

    @Value("${services.time-logging.url}")
    private String timeLoggingServiceUrl;

    @Value("${services.appointment-booking.url}")
    private String appointmentBookingServiceUrl;

    @Value("${services.service-tracking.url}")
    private String serviceTrackingServiceUrl;

    public Mono<OperationalViewResponse> getOperationalView(Long employeeId) {
        log.info("Fetching operational view for employee: {}", employeeId);

        Mono<OperationalViewResponse.ActiveTimerData> activeTimerMono = getActiveTimer(employeeId);
        Mono<List<OperationalViewResponse.AppointmentData>> appointmentsMono = getTodaysAppointments(employeeId);
        Mono<List<OperationalViewResponse.WorkQueueItem>> workQueueMono = getWorkQueue(employeeId);

        return Mono.zip(activeTimerMono, appointmentsMono, workQueueMono)
                .map(tuple -> OperationalViewResponse.builder()
                        .activeTimer(tuple.getT1())
                        .todaysAppointments(tuple.getT2())
                        .workQueue(tuple.getT3())
                        .build())
                .doOnError(error -> log.error("Error fetching operational view: {}", error.getMessage()));
    }

    private Mono<OperationalViewResponse.ActiveTimerData> getActiveTimer(Long employeeId) {
        log.debug("Fetching active timer for employee: {}", employeeId);
        
        return webClientBuilder.build()
                .get()
                .uri(timeLoggingServiceUrl + "/api/time-logging/active?employeeId=" + employeeId)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::mapToActiveTimer)
                .onErrorResume(error -> {
                    log.warn("Error fetching active timer: {}", error.getMessage());
                    return Mono.just(OperationalViewResponse.ActiveTimerData.builder().build());
                });
    }

    private Mono<List<OperationalViewResponse.AppointmentData>> getTodaysAppointments(Long employeeId) {
        log.debug("Fetching today's appointments for employee: {}", employeeId);
        
        return webClientBuilder.build()
                .get()
                .uri(appointmentBookingServiceUrl + "/api/appointments/today?employeeId=" + employeeId)
                .retrieve()
                .bodyToFlux(Map.class)
                .map(this::mapToAppointment)
                .collectList()
                .onErrorResume(error -> {
                    log.warn("Error fetching appointments: {}", error.getMessage());
                    return Mono.just(List.of());
                });
    }

    private Mono<List<OperationalViewResponse.WorkQueueItem>> getWorkQueue(Long employeeId) {
        log.debug("Fetching work queue for employee: {}", employeeId);
        
        return webClientBuilder.build()
                .get()
                .uri(serviceTrackingServiceUrl + "/api/service-tracking/work-queue?employeeId=" + employeeId)
                .retrieve()
                .bodyToFlux(Map.class)
                .map(this::mapToWorkQueueItem)
                .collectList()
                .onErrorResume(error -> {
                    log.warn("Error fetching work queue: {}", error.getMessage());
                    return Mono.just(List.of());
                });
    }

    private OperationalViewResponse.ActiveTimerData mapToActiveTimer(Map<String, Object> data) {
        return OperationalViewResponse.ActiveTimerData.builder()
                .timerId(getLongValue(data, "timerId"))
                .employeeId(getLongValue(data, "employeeId"))
                .jobId(getStringValue(data, "jobId"))
                .status(getStringValue(data, "status"))
                .startTime(getStringValue(data, "startTime"))
                .elapsedSeconds(getLongValue(data, "elapsedSeconds"))
                .build();
    }

    private OperationalViewResponse.AppointmentData mapToAppointment(Map<String, Object> data) {
        return OperationalViewResponse.AppointmentData.builder()
                .appointmentId(getLongValue(data, "appointmentId"))
                .customerName(getStringValue(data, "customerName"))
                .appointmentTime(getStringValue(data, "appointmentTime"))
                .serviceType(getStringValue(data, "serviceType"))
                .status(getStringValue(data, "status"))
                .build();
    }

    private OperationalViewResponse.WorkQueueItem mapToWorkQueueItem(Map<String, Object> data) {
        return OperationalViewResponse.WorkQueueItem.builder()
                .jobId(getLongValue(data, "jobId"))
                .jobType(getStringValue(data, "jobType"))
                .priority(getStringValue(data, "priority"))
                .status(getStringValue(data, "status"))
                .assignedTo(getStringValue(data, "assignedTo"))
                .deadline(getStringValue(data, "deadline"))
                .build();
    }

    private String getStringValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }

    private Long getLongValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }
}
