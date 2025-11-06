package com.autonova.customer.controller;

import com.autonova.customer.dto.VehicleRequest;
import com.autonova.customer.dto.VehicleResponse;
import com.autonova.customer.service.VehicleService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Convenience endpoints for interacting with the authenticated customer's vehicles.
 */
@RestController
@RequestMapping("/api/customers/me/vehicles")
public class CurrentCustomerVehicleController {

    private final VehicleService vehicleService;

    public CurrentCustomerVehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleResponse addVehicle(@Valid @RequestBody VehicleRequest request) {
        return vehicleService.addVehicleForCurrentCustomer(request);
    }

    @GetMapping
    public List<VehicleResponse> listVehicles() {
        return vehicleService.getVehiclesForCurrentCustomer();
    }

    @GetMapping("/{vehicleId}")
    public VehicleResponse getVehicle(@PathVariable Long vehicleId) {
        return vehicleService.getVehicleForCurrentCustomer(vehicleId);
    }

    @PutMapping("/{vehicleId}")
    public VehicleResponse updateVehicle(@PathVariable Long vehicleId,
                                         @Valid @RequestBody VehicleRequest request) {
        return vehicleService.updateVehicleForCurrentCustomer(vehicleId, request);
    }

    @DeleteMapping("/{vehicleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteVehicle(@PathVariable Long vehicleId) {
        vehicleService.deleteVehicleForCurrentCustomer(vehicleId);
    }
}
