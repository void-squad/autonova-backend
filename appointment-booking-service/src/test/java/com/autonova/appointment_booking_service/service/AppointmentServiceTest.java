package com.autonova.appointment_booking_service.service;

import com.autonova.appointment_booking_service.dto.AppointmentRequestDto;
import com.autonova.appointment_booking_service.dto.AppointmentResponseDto;
import com.autonova.appointment_booking_service.entity.Appointment;
import com.autonova.appointment_booking_service.exception.ConflictException;
import com.autonova.appointment_booking_service.messaging.AppointmentEventPublisher;
import com.autonova.appointment_booking_service.repository.AppointmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository repository;

    @Mock
    private AppointmentEventPublisher eventPublisher;

    @InjectMocks
    private AppointmentService appointmentService;

    private UUID customerId;
    private UUID vehicleId;
    private UUID employeeId;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        vehicleId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        startTime = OffsetDateTime.now().plusDays(1);
        endTime = startTime.plusHours(2);
    }

    @Test
    void createAppointment_withValidRequest_createsAppointment() {
        // Given
        AppointmentRequestDto request = new AppointmentRequestDto();
        request.setCustomerId(customerId);
        request.setCustomerUsername("test@example.com");
        request.setVehicleId(vehicleId);
        request.setVehicleName("Honda Civic");
        request.setServiceType("Oil Change");
        request.setStartTime(startTime);
        request.setEndTime(endTime);
        request.setNotes("Test notes");

        when(repository.findOverlappingForVehicle(vehicleId, startTime, endTime))
                .thenReturn(Collections.emptyList());
        when(repository.findInTimeRange(startTime, endTime))
                .thenReturn(Collections.emptyList());
        when(repository.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        AppointmentResponseDto result = appointmentService.createAppointment(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCustomerId()).isEqualTo(customerId);
        assertThat(result.getVehicleId()).isEqualTo(vehicleId);
        assertThat(result.getServiceType()).isEqualTo("Oil Change");
        assertThat(result.getStatus()).isEqualTo("PENDING");
        
        verify(repository).save(any(Appointment.class));
        verify(eventPublisher).publish(eq("appointment.created"), any(Appointment.class));
    }

    @Test
    void createAppointment_withEndTimeBeforeStartTime_throwsIllegalArgumentException() {
        // Given
        AppointmentRequestDto request = new AppointmentRequestDto();
        request.setCustomerId(customerId);
        request.setVehicleId(vehicleId);
        request.setServiceType("Test");
        request.setStartTime(startTime);
        request.setEndTime(startTime.minusHours(1)); // End before start

        // When/Then
        assertThatThrownBy(() -> appointmentService.createAppointment(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("endTime must be after startTime");

        verify(repository, never()).save(any());
    }

    @Test
    void createAppointment_withVehicleConflict_throwsConflictException() {
        // Given
        AppointmentRequestDto request = new AppointmentRequestDto();
        request.setCustomerId(customerId);
        request.setVehicleId(vehicleId);
        request.setServiceType("Test");
        request.setStartTime(startTime);
        request.setEndTime(endTime);

        Appointment existingAppointment = createTestAppointment();
        when(repository.findOverlappingForVehicle(vehicleId, startTime, endTime))
                .thenReturn(Collections.singletonList(existingAppointment));

        // When/Then
        assertThatThrownBy(() -> appointmentService.createAppointment(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Vehicle already has an appointment");

        verify(repository, never()).save(any());
    }

    @Test
    void createAppointment_withEmployeeConflict_throwsConflictException() {
        // Given
        AppointmentRequestDto request = new AppointmentRequestDto();
        request.setCustomerId(customerId);
        request.setVehicleId(vehicleId);
        request.setServiceType("Test");
        request.setStartTime(startTime);
        request.setEndTime(endTime);
        request.setPreferredEmployeeId(employeeId);

        Appointment existingAppointment = createTestAppointment();
        when(repository.findOverlappingForVehicle(vehicleId, startTime, endTime))
                .thenReturn(Collections.emptyList());
        when(repository.findOverlappingForEmployee(employeeId, startTime, endTime))
                .thenReturn(Collections.singletonList(existingAppointment));

        // When/Then
        assertThatThrownBy(() -> appointmentService.createAppointment(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Preferred employee is unavailable");

        verify(repository, never()).save(any());
    }

    @Test
    void createAppointment_withCapacityFull_throwsConflictException() {
        // Given
        AppointmentRequestDto request = new AppointmentRequestDto();
        request.setCustomerId(customerId);
        request.setVehicleId(vehicleId);
        request.setServiceType("Test");
        request.setStartTime(startTime);
        request.setEndTime(endTime);

        // Return 3 appointments (capacity = 3)
        List<Appointment> overlaps = Arrays.asList(
                createTestAppointment(),
                createTestAppointment(),
                createTestAppointment()
        );

        when(repository.findOverlappingForVehicle(vehicleId, startTime, endTime))
                .thenReturn(Collections.emptyList());
        when(repository.findInTimeRange(startTime, endTime))
                .thenReturn(overlaps);

        // When/Then
        assertThatThrownBy(() -> appointmentService.createAppointment(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("No available service capacity");

        verify(repository, never()).save(any());
    }

    @Test
    void reschedule_withValidNewTime_reschedulesAppointment() {
        // Given
        UUID appointmentId = UUID.randomUUID();
        Appointment existingAppointment = createTestAppointment();
        existingAppointment.setId(appointmentId);
        existingAppointment.setStatus("PENDING");
        
        OffsetDateTime newStart = startTime.plusDays(1);
        OffsetDateTime newEnd = endTime.plusDays(1);

        when(repository.findById(appointmentId)).thenReturn(Optional.of(existingAppointment));
        when(repository.findOverlappingForVehicle(any(), eq(newStart), eq(newEnd)))
                .thenReturn(Collections.emptyList());
        when(repository.findInTimeRange(newStart, newEnd))
                .thenReturn(Collections.emptyList());
        when(repository.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        AppointmentResponseDto result = appointmentService.reschedule(appointmentId, newStart, newEnd);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStartTime()).isEqualTo(newStart);
        assertThat(result.getEndTime()).isEqualTo(newEnd);
        
        verify(repository).save(any(Appointment.class));
        verify(eventPublisher).publish(eq("appointment.rescheduled"), any(Appointment.class));
    }

    @Test
    void reschedule_cancelledAppointment_throwsIllegalStateException() {
        // Given
        UUID appointmentId = UUID.randomUUID();
        Appointment cancelledAppointment = createTestAppointment();
        cancelledAppointment.setId(appointmentId);
        cancelledAppointment.setStatus("CANCELLED");
        
        when(repository.findById(appointmentId)).thenReturn(Optional.of(cancelledAppointment));

        // When/Then
        assertThatThrownBy(() -> appointmentService.reschedule(appointmentId, startTime, endTime))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot reschedule a cancelled appointment");

        verify(repository, never()).save(any());
    }

    @Test
    void cancel_existingAppointment_cancelsAppointment() {
        // Given
        UUID appointmentId = UUID.randomUUID();
        UUID cancelledBy = UUID.randomUUID();
        Appointment appointment = createTestAppointment();
        appointment.setId(appointmentId);
        appointment.setStatus("PENDING");

        when(repository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(repository.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        appointmentService.cancel(appointmentId, cancelledBy);

        // Then
        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("CANCELLED");
        
        verify(eventPublisher).publish(eq("appointment.cancelled"), any(Appointment.class));
    }

    @Test
    void listByCustomer_returnsCustomerAppointments() {
        // Given
        List<Appointment> appointments = Arrays.asList(
                createTestAppointment(),
                createTestAppointment()
        );
        
        when(repository.findByCustomerIdOrderByStartTimeDesc(customerId))
                .thenReturn(appointments);

        // When
        List<AppointmentResponseDto> result = appointmentService.listByCustomer(customerId);

        // Then
        assertThat(result).hasSize(2);
        verify(repository).findByCustomerIdOrderByStartTimeDesc(customerId);
    }

    @Test
    void getById_existingAppointment_returnsAppointment() {
        // Given
        UUID appointmentId = UUID.randomUUID();
        Appointment appointment = createTestAppointment();
        appointment.setId(appointmentId);
        
        when(repository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        // When
        AppointmentResponseDto result = appointmentService.getById(appointmentId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(appointmentId);
    }

    @Test
    void getById_nonExistentAppointment_throwsNoSuchElementException() {
        // Given
        UUID appointmentId = UUID.randomUUID();
        when(repository.findById(appointmentId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> appointmentService.getById(appointmentId))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Appointment not found");
    }

    @Test
    void listAll_returnsAllAppointments() {
        // Given
        List<Appointment> appointments = Arrays.asList(
                createTestAppointment(),
                createTestAppointment(),
                createTestAppointment()
        );
        
        when(repository.findAll()).thenReturn(appointments);

        // When
        List<AppointmentResponseDto> result = appointmentService.listAll();

        // Then
        assertThat(result).hasSize(3);
        verify(repository).findAll();
    }

    @Test
    void updateStatus_updatesAppointmentStatus() {
        // Given
        UUID appointmentId = UUID.randomUUID();
        Appointment appointment = createTestAppointment();
        appointment.setId(appointmentId);
        appointment.setStatus("PENDING");
        
        when(repository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(repository.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        AppointmentResponseDto result = appointmentService.updateStatus(
                appointmentId, "CONFIRMED", "Admin approved");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("CONFIRMED");
        
        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("CONFIRMED");
        assertThat(captor.getValue().getNotes()).contains("Admin approved");
    }

    @Test
    void assignEmployee_assignsEmployeeToAppointment() {
        // Given
        UUID appointmentId = UUID.randomUUID();
        Appointment appointment = createTestAppointment();
        appointment.setId(appointmentId);
        
        when(repository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(repository.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        AppointmentResponseDto result = appointmentService.assignEmployee(appointmentId, employeeId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAssignedEmployeeId()).isEqualTo(employeeId);
        
        verify(repository).save(any(Appointment.class));
    }

    private Appointment createTestAppointment() {
        return Appointment.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .customerUsername("test@example.com")
                .vehicleId(vehicleId)
                .vehicleName("Test Vehicle")
                .serviceType("Test Service")
                .startTime(startTime)
                .endTime(endTime)
                .status("PENDING")
                .createdAt(OffsetDateTime.now())
                .build();
    }
}
