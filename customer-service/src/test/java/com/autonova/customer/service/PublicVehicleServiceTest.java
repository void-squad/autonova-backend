package com.autonova.customer.service;

import com.autonova.customer.dto.VehicleDetailsDto;
import com.autonova.customer.model.Vehicle;
import com.autonova.customer.repository.VehicleRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicVehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private VehicleService vehicleService;

    @Test
    void getVehicleDetails_returnsVehicleDetailsDto() {
        Long vehicleId = 42L;
        Vehicle vehicle = new Vehicle();
        vehicle.setId(vehicleId);
        vehicle.setMake("Toyota");
        vehicle.setModel("Camry");
        vehicle.setYear(2020);
        vehicle.setLicensePlate("ABC-123");
        vehicle.setVin("VINVINVIN123");
        vehicle.setCreatedAt(LocalDateTime.now());
        vehicle.setUpdatedAt(LocalDateTime.now());

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));

        VehicleDetailsDto details = vehicleService.getVehicleDetails(vehicleId);

        assertThat(details).isNotNull();
        assertThat(details.id()).isEqualTo(vehicleId);
        assertThat(details.licensePlate()).isEqualTo("ABC-123");
        assertThat(details.make()).isEqualTo("Toyota");
        assertThat(details.model()).isEqualTo("Camry");
        assertThat(details.year()).isEqualTo(2020);
        assertThat(details.vin()).isEqualTo("VINVINVIN123");
    }

    @Test
    void getVehicleDetails_throwsNotFoundWhenVehicleDoesNotExist() {
        Long vehicleId = 999L;
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehicleService.getVehicleDetails(vehicleId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }
}
