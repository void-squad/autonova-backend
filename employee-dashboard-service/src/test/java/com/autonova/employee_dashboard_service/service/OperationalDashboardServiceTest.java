package com.autonova.employee_dashboard_service.service;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.autonova.employee_dashboard_service.dto.OperationalViewResponse;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class OperationalDashboardServiceTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private OperationalDashboardService operationalDashboardService;

    private Long employeeId;

    @BeforeEach
    void setUp() {
        employeeId = 1L;
        ReflectionTestUtils.setField(operationalDashboardService, "timeLoggingServiceUrl", "http://localhost:8081");
        ReflectionTestUtils.setField(operationalDashboardService, "appointmentBookingServiceUrl", "http://localhost:8082");
        ReflectionTestUtils.setField(operationalDashboardService, "serviceTrackingServiceUrl", "http://localhost:8083");
    }

    @Test
    void getOperationalView_ShouldReturnCompleteData() {
        // Arrange
        Map<String, Object> timerData = new HashMap<>();
        timerData.put("timerId", 1);
        timerData.put("employeeId", 1);
        timerData.put("jobId", "JOB-123");
        timerData.put("status", "RUNNING");
        timerData.put("startTime", "2025-11-06T10:00:00");
        timerData.put("elapsedSeconds", 3600);

        Map<String, Object> appointmentData = new HashMap<>();
        appointmentData.put("appointmentId", 1);
        appointmentData.put("customerName", "John Doe");
        appointmentData.put("appointmentTime", "2025-11-06T14:00:00");
        appointmentData.put("serviceType", "Oil Change");
        appointmentData.put("status", "CONFIRMED");

        Map<String, Object> workQueueData = new HashMap<>();
        workQueueData.put("jobId", 1);
        workQueueData.put("jobType", "Repair");
        workQueueData.put("priority", "HIGH");
        workQueueData.put("status", "PENDING");
        workQueueData.put("assignedTo", "Employee 1");
        workQueueData.put("deadline", "2025-11-07T17:00:00");

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        
        // Setup responses for different endpoints
        when(responseSpec.bodyToMono(Map.class))
                .thenReturn(Mono.just(timerData));
        
        when(responseSpec.bodyToFlux(Map.class))
                .thenReturn(Flux.just(appointmentData))
                .thenReturn(Flux.just(workQueueData));

        // Act
        Mono<OperationalViewResponse> result = operationalDashboardService.getOperationalView(employeeId);

        // Assert
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    
                    // Verify active timer
                    assertThat(response.getActiveTimer()).isNotNull();
                    assertThat(response.getActiveTimer().getTimerId()).isEqualTo(1L);
                    assertThat(response.getActiveTimer().getJobId()).isEqualTo("JOB-123");
                    assertThat(response.getActiveTimer().getStatus()).isEqualTo("RUNNING");
                    
                    // Verify appointments
                    assertThat(response.getTodaysAppointments()).isNotNull();
                    assertThat(response.getTodaysAppointments()).hasSize(1);
                    assertThat(response.getTodaysAppointments().get(0).getCustomerName()).isEqualTo("John Doe");
                    
                    // Verify work queue
                    assertThat(response.getWorkQueue()).isNotNull();
                    assertThat(response.getWorkQueue()).hasSize(1);
                    assertThat(response.getWorkQueue().get(0).getJobType()).isEqualTo("Repair");
                })
                .verifyComplete();
    }

    @Test
    void getOperationalView_WhenActiveTimerFails_ShouldReturnEmptyTimer() {
        // Arrange
        Map<String, Object> appointmentData = new HashMap<>();
        appointmentData.put("appointmentId", 1);
        appointmentData.put("customerName", "John Doe");

        Map<String, Object> workQueueData = new HashMap<>();
        workQueueData.put("jobId", 1);
        workQueueData.put("jobType", "Repair");

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        
        when(responseSpec.bodyToMono(Map.class))
                .thenReturn(Mono.error(new RuntimeException("Service unavailable")));
        
        when(responseSpec.bodyToFlux(Map.class))
                .thenReturn(Flux.just(appointmentData))
                .thenReturn(Flux.just(workQueueData));

        // Act
        Mono<OperationalViewResponse> result = operationalDashboardService.getOperationalView(employeeId);

        // Assert
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.getActiveTimer()).isNotNull();
                    assertThat(response.getActiveTimer().getTimerId()).isNull();
                })
                .verifyComplete();
    }

    @Test
    void getOperationalView_WhenAppointmentsFail_ShouldReturnEmptyList() {
        // Arrange
        Map<String, Object> timerData = new HashMap<>();
        timerData.put("timerId", 1);

        Map<String, Object> workQueueData = new HashMap<>();
        workQueueData.put("jobId", 1);

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        
        when(responseSpec.bodyToMono(Map.class))
                .thenReturn(Mono.just(timerData));
        
        when(responseSpec.bodyToFlux(Map.class))
                .thenReturn(Flux.error(new RuntimeException("Service unavailable")))
                .thenReturn(Flux.just(workQueueData));

        // Act
        Mono<OperationalViewResponse> result = operationalDashboardService.getOperationalView(employeeId);

        // Assert
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.getTodaysAppointments()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    void getOperationalView_WhenWorkQueueFails_ShouldReturnEmptyList() {
        // Arrange
        Map<String, Object> timerData = new HashMap<>();
        timerData.put("timerId", 1);

        Map<String, Object> appointmentData = new HashMap<>();
        appointmentData.put("appointmentId", 1);

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        
        when(responseSpec.bodyToMono(Map.class))
                .thenReturn(Mono.just(timerData));
        
        when(responseSpec.bodyToFlux(Map.class))
                .thenReturn(Flux.just(appointmentData))
                .thenReturn(Flux.error(new RuntimeException("Service unavailable")));

        // Act
        Mono<OperationalViewResponse> result = operationalDashboardService.getOperationalView(employeeId);

        // Assert
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.getWorkQueue()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    void getOperationalView_WithMultipleAppointments_ShouldReturnAll() {
        // Arrange
        Map<String, Object> timerData = new HashMap<>();
        timerData.put("timerId", 1);

        Map<String, Object> appointment1 = new HashMap<>();
        appointment1.put("appointmentId", 1);
        appointment1.put("customerName", "John Doe");

        Map<String, Object> appointment2 = new HashMap<>();
        appointment2.put("appointmentId", 2);
        appointment2.put("customerName", "Jane Smith");

        Map<String, Object> workQueueData = new HashMap<>();
        workQueueData.put("jobId", 1);

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        
        when(responseSpec.bodyToMono(Map.class))
                .thenReturn(Mono.just(timerData));
        
        when(responseSpec.bodyToFlux(Map.class))
                .thenReturn(Flux.just(appointment1, appointment2))
                .thenReturn(Flux.just(workQueueData));

        // Act
        Mono<OperationalViewResponse> result = operationalDashboardService.getOperationalView(employeeId);

        // Assert
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.getTodaysAppointments()).hasSize(2);
                    assertThat(response.getTodaysAppointments().get(0).getCustomerName()).isEqualTo("John Doe");
                    assertThat(response.getTodaysAppointments().get(1).getCustomerName()).isEqualTo("Jane Smith");
                })
                .verifyComplete();
    }

    @Test
    void getOperationalView_WithNullValues_ShouldHandleGracefully() {
        // Arrange
        Map<String, Object> timerData = new HashMap<>();
        timerData.put("timerId", null);
        timerData.put("employeeId", null);
        timerData.put("jobId", null);

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        
        when(responseSpec.bodyToMono(Map.class))
                .thenReturn(Mono.just(timerData));
        
        when(responseSpec.bodyToFlux(Map.class))
                .thenReturn(Flux.empty())
                .thenReturn(Flux.empty());

        // Act
        Mono<OperationalViewResponse> result = operationalDashboardService.getOperationalView(employeeId);

        // Assert
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.getActiveTimer()).isNotNull();
                    assertThat(response.getActiveTimer().getTimerId()).isNull();
                    assertThat(response.getActiveTimer().getJobId()).isNull();
                    assertThat(response.getTodaysAppointments()).isEmpty();
                    assertThat(response.getWorkQueue()).isEmpty();
                })
                .verifyComplete();
    }
}
