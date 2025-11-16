package com.autonova.customer.service;

import com.autonova.customer.dto.VehicleRequest;
import com.autonova.customer.dto.VehicleResponse;
import com.autonova.customer.dto.VehicleStatsResponse;
import com.autonova.customer.event.VehicleDomainEventPublisher;
import com.autonova.customer.event.VehicleEventType;
import com.autonova.customer.model.Customer;
import com.autonova.customer.model.Vehicle;
import com.autonova.customer.repository.CustomerRepository;
import com.autonova.customer.repository.VehicleRepository;
import com.autonova.customer.security.AuthenticatedUser;
import com.autonova.customer.security.CurrentUserProvider;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    private static final AuthenticatedUser ADMIN = new AuthenticatedUser(1L, "admin@autonova.com", "ADMIN");
    private static final AuthenticatedUser CUSTOMER_USER = new AuthenticatedUser(2L, "ada@example.com", "CUSTOMER");

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private VehicleDomainEventPublisher vehicleDomainEventPublisher;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private VehicleService vehicleService;

    @BeforeEach
    void setUp() {
        vehicleService = new VehicleService(customerRepository, vehicleRepository,
                vehicleDomainEventPublisher, currentUserProvider);
    }

    @Test
    void addVehicle_throwsConflictWhenVinExists() {
        Long customerId = 55L;
        VehicleRequest request = new VehicleRequest("Honda", "Civic", 2024, "vin-123", "abc123");

        Customer customer = new Customer();
        customer.setId(customerId);
        when(currentUserProvider.requireCurrentUser()).thenReturn(ADMIN);
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(vehicleRepository.existsByVinIgnoreCase("VIN-123")).thenReturn(true);

        assertThatThrownBy(() -> vehicleService.addVehicle(customerId, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));

        verify(vehicleRepository, never()).saveAndFlush(any(Vehicle.class));
        verify(vehicleDomainEventPublisher, never()).publish(any(VehicleEventType.class), any(Vehicle.class));
    }

    @Test
    void updateVehicle_normalizesIdentifiersAndPublishesEvent() {
        Long customerId = 77L;
        Long vehicleId = 9L;
        VehicleRequest request = new VehicleRequest("Tesla", "Model Y", 2025, "newvin123", "new-plate");

        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setCreatedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());

        Vehicle vehicle = new Vehicle();
        vehicle.setId(vehicleId);
        vehicle.setCustomer(customer);
        vehicle.setMake("Old");
        vehicle.setModel("Model");
        vehicle.setYear(2020);
        vehicle.setVin("OLDVIN");
        vehicle.setLicensePlate("OLD-PLATE");
        vehicle.setCreatedAt(LocalDateTime.now());
        vehicle.setUpdatedAt(LocalDateTime.now());

        when(currentUserProvider.requireCurrentUser()).thenReturn(ADMIN);
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(vehicleRepository.findByCustomerIdAndId(customerId, vehicleId)).thenReturn(Optional.of(vehicle));
        when(vehicleRepository.existsByVinIgnoreCaseAndIdNot("NEWVIN123", vehicleId)).thenReturn(false);
        when(vehicleRepository.existsByCustomerIdAndLicensePlateIgnoreCaseAndIdNot(customerId, "NEW-PLATE", vehicleId))
                .thenReturn(false);
        when(vehicleRepository.saveAndFlush(vehicle)).thenReturn(vehicle);

        VehicleResponse response = vehicleService.updateVehicle(customerId, vehicleId, request);

        assertThat(vehicle.getVin()).isEqualTo("NEWVIN123");
        assertThat(vehicle.getLicensePlate()).isEqualTo("NEW-PLATE");
        assertThat(response.vin()).isEqualTo("NEWVIN123");
        assertThat(response.licensePlate()).isEqualTo("NEW-PLATE");
        assertThat(response.customerId()).isEqualTo(customerId);

        verify(vehicleRepository).saveAndFlush(vehicle);
        verify(vehicleDomainEventPublisher).publish(VehicleEventType.UPDATED, vehicle);
    }

    @Test
    void getVehicleStatsForCurrentCustomerReturnsCount() {
        Customer customer = new Customer();
        customer.setId(42L);
        customer.setEmail("ada@example.com");

        when(currentUserProvider.requireCurrentUser()).thenReturn(CUSTOMER_USER);
        when(customerRepository.findByEmailIgnoreCase("ada@example.com"))
                .thenReturn(Optional.of(customer));
        when(vehicleRepository.countByCustomerId(42L)).thenReturn(5L);

        VehicleStatsResponse stats = vehicleService.getVehicleStatsForCurrentCustomer();

        assertThat(stats.totalVehicles()).isEqualTo(5L);
        assertThat(stats.generatedAt()).isNotNull();
        verify(vehicleRepository).countByCustomerId(42L);
    }

    @Test
    void getVehicleStatsWithCustomerIdChecksAccess() {
        Long customerId = 99L;
        Customer customer = new Customer();
        customer.setId(customerId);

        when(currentUserProvider.requireCurrentUser()).thenReturn(ADMIN);
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(vehicleRepository.countByCustomerId(customerId)).thenReturn(2L);

        VehicleStatsResponse stats = vehicleService.getVehicleStats(customerId);

        assertThat(stats.totalVehicles()).isEqualTo(2L);
        verify(customerRepository).findById(customerId);
        verify(vehicleRepository).countByCustomerId(customerId);
    }
}
