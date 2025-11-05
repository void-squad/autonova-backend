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

@RestController
@RequestMapping("/api/customers/{customerId}/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleResponse addVehicle(@PathVariable Long customerId, @Valid @RequestBody VehicleRequest request) {
        return vehicleService.addVehicle(customerId, request);
    }

    @GetMapping
    public List<VehicleResponse> listVehicles(@PathVariable Long customerId) {
        return vehicleService.getVehicles(customerId);
    }

    @GetMapping("/{vehicleId}")
    public VehicleResponse getVehicle(@PathVariable Long customerId, @PathVariable Long vehicleId) {
        return vehicleService.getVehicle(customerId, vehicleId);
    }

    @PutMapping("/{vehicleId}")
    public VehicleResponse updateVehicle(@PathVariable Long customerId,
                                         @PathVariable Long vehicleId,
                                         @Valid @RequestBody VehicleRequest request) {
        return vehicleService.updateVehicle(customerId, vehicleId, request);
    }

    @DeleteMapping("/{vehicleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteVehicle(@PathVariable Long customerId, @PathVariable Long vehicleId) {
        vehicleService.deleteVehicle(customerId, vehicleId);
    }
}
