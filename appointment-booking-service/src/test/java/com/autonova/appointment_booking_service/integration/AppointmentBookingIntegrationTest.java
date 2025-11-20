package com.autonova.appointment_booking_service.integration;

import com.autonova.appointment_booking_service.entity.Appointment;
import com.autonova.appointment_booking_service.repository.AppointmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Appointment Booking Service with PostgreSQL and RabbitMQ using Testcontainers
 */
@SpringBootTest
@Testcontainers
@Transactional
class AppointmentBookingIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("appointmentdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management-alpine")
            .withExposedPorts(5672, 15672);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");

        registry.add("jwt.secret", () -> "testSecretKeyForJwtTokenGenerationAndValidationInTestEnvironmentMinimum256Bits");
    }

    @Autowired
    private AppointmentRepository appointmentRepository;

    @BeforeEach
    void setUp() {
        appointmentRepository.deleteAll();
    }

    @Test
    void testContainersAreRunning() {
        assertThat(postgres.isRunning()).isTrue();
        assertThat(rabbitmq.isRunning()).isTrue();
    }

    @Test
    void createAndRetrieveAppointment_worksCorrectly() {
        // Given
        Appointment appointment = Appointment.builder()
                .id(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .customerUsername("john.doe@example.com")
                .vehicleId(UUID.randomUUID())
                .vehicleName("Toyota Camry")
                .serviceType("Oil Change")
                .startTime(OffsetDateTime.now().plusDays(1))
                .endTime(OffsetDateTime.now().plusDays(1).plusHours(2))
                .status("PENDING")
                .createdAt(OffsetDateTime.now())
                .build();
        
        // When
        appointment = appointmentRepository.save(appointment);
        Appointment retrieved = appointmentRepository.findById(appointment.getId()).orElse(null);

        // Then
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getCustomerUsername()).isEqualTo("john.doe@example.com");
        assertThat(retrieved.getServiceType()).isEqualTo("Oil Change");
        assertThat(retrieved.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void updateAppointmentStatus_persistsChanges() {
        // Given
        Appointment appointment = Appointment.builder()
                .id(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .customerUsername("jane.smith@example.com")
                .vehicleId(UUID.randomUUID())
                .vehicleName("Honda Accord")
                .serviceType("Brake Inspection")
                .startTime(OffsetDateTime.now().plusDays(2))
                .endTime(OffsetDateTime.now().plusDays(2).plusHours(1))
                .status("PENDING")
                .createdAt(OffsetDateTime.now())
                .build();
        appointment = appointmentRepository.save(appointment);

        // When
        appointment.setStatus("CONFIRMED");
        appointment.setUpdatedAt(OffsetDateTime.now());
        appointmentRepository.save(appointment);

        // Then
        Appointment updated = appointmentRepository.findById(appointment.getId()).orElse(null);
        assertThat(updated).isNotNull();
        assertThat(updated.getStatus()).isEqualTo("CONFIRMED");
        assertThat(updated.getUpdatedAt()).isNotNull();
    }

    @Test
    void deleteAppointment_removesFromDatabase() {
        // Given
        Appointment appointment = Appointment.builder()
                .id(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .customerUsername("bob.johnson@example.com")
                .vehicleId(UUID.randomUUID())
                .vehicleName("Ford F-150")
                .serviceType("Tire Rotation")
                .startTime(OffsetDateTime.now().plusDays(3))
                .endTime(OffsetDateTime.now().plusDays(3).plusHours(1))
                .status("PENDING")
                .createdAt(OffsetDateTime.now())
                .build();
        appointment = appointmentRepository.save(appointment);
        UUID appointmentId = appointment.getId();

        // When
        appointmentRepository.deleteById(appointmentId);

        // Then
        assertThat(appointmentRepository.findById(appointmentId)).isEmpty();
    }

    @Test
    void findByCustomerId_returnsCustomerAppointments() {
        // Given
        UUID customerId = UUID.randomUUID();
        
        Appointment appointment1 = Appointment.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .customerUsername("alice@example.com")
                .vehicleId(UUID.randomUUID())
                .vehicleName("Tesla Model 3")
                .serviceType("Battery Check")
                .startTime(OffsetDateTime.now().plusDays(1))
                .endTime(OffsetDateTime.now().plusDays(1).plusHours(1))
                .status("PENDING")
                .createdAt(OffsetDateTime.now())
                .build();
        appointmentRepository.save(appointment1);

        Appointment appointment2 = Appointment.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .customerUsername("alice@example.com")
                .vehicleId(UUID.randomUUID())
                .vehicleName("Tesla Model 3")
                .serviceType("Software Update")
                .startTime(OffsetDateTime.now().plusDays(7))
                .endTime(OffsetDateTime.now().plusDays(7).plusMinutes(30))
                .status("CONFIRMED")
                .createdAt(OffsetDateTime.now())
                .build();
        appointmentRepository.save(appointment2);

        // When
        List<Appointment> appointments = appointmentRepository.findByCustomerIdOrderByStartTimeDesc(customerId);

        // Then
        assertThat(appointments).hasSize(2);
        assertThat(appointments).extracting(Appointment::getCustomerId)
                .containsOnly(customerId);
    }

    @Test
    void multipleAppointments_canBeStoredAndRetrieved() {
        // Given
        for (int i = 0; i < 5; i++) {
            Appointment appointment = Appointment.builder()
                    .id(UUID.randomUUID())
                    .customerId(UUID.randomUUID())
                    .customerUsername("customer" + i + "@example.com")
                    .vehicleId(UUID.randomUUID())
                    .vehicleName("Vehicle " + i)
                    .serviceType("Service Type " + i)
                    .startTime(OffsetDateTime.now().plusDays(i + 1))
                    .endTime(OffsetDateTime.now().plusDays(i + 1).plusHours(1))
                    .status("PENDING")
                    .createdAt(OffsetDateTime.now())
                    .build();
            appointmentRepository.save(appointment);
        }

        // When
        long count = appointmentRepository.count();

        // Then
        assertThat(count).isEqualTo(5);
    }

    @Test
    void assignEmployee_updatesAppointment() {
        // Given
        Appointment appointment = Appointment.builder()
                .id(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .customerUsername("charlie@example.com")
                .vehicleId(UUID.randomUUID())
                .vehicleName("BMW X5")
                .serviceType("Full Service")
                .startTime(OffsetDateTime.now().plusDays(5))
                .endTime(OffsetDateTime.now().plusDays(5).plusHours(3))
                .status("CONFIRMED")
                .createdAt(OffsetDateTime.now())
                .build();
        appointment = appointmentRepository.save(appointment);

        // When
        UUID employeeId = UUID.randomUUID();
        appointment.setAssignedEmployeeId(employeeId);
        appointment.setStatus("IN_PROGRESS");
        appointmentRepository.save(appointment);

        // Then
        Appointment updated = appointmentRepository.findById(appointment.getId()).orElse(null);
        assertThat(updated).isNotNull();
        assertThat(updated.getAssignedEmployeeId()).isEqualTo(employeeId);
        assertThat(updated.getStatus()).isEqualTo("IN_PROGRESS");
    }
}
