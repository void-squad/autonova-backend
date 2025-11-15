package com.autonova.customer.controller;

import com.autonova.customer.dto.VehicleDetailsDto;
import com.autonova.customer.service.VehicleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public Vehicle API for cross-service consumption.
 * 
 * Exposes vehicle details endpoints that other services (e.g., Project Service)
 * can consume to fetch vehicle information without customer authentication.
 * 
 * Use Case: Project Service needs to display vehicle details like:
 * "2020 Toyota Camry (ABC-123)"
 */
@RestController
@RequestMapping("/api/vehicles")
public class PublicVehicleController {

    private final VehicleService vehicleService;

    public PublicVehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    /**
     * Get vehicle details by vehicle ID.
     * 
     * This endpoint is public and accessible to all services (authenticated or not)
     * to retrieve essential vehicle information for display purposes.
     * 
     * @param vehicleId the vehicle identifier
    * @return VehicleDetailsDto containing id, licensePlate, make, model, year, vin
     * @throws org.springframework.web.server.ResponseStatusException 404 if vehicle not found
     * 
     * Example Response:
     * {
     *   "id": 123,
     *   "licensePlate": "ABC-123",
     *   "make": "Toyota",
     *   "model": "Camry",
    *   "year": 2020,
    *   "vin": "VINVINVIN123"
     * }
     */
    @GetMapping("/{vehicleId}")
    public VehicleDetailsDto getVehicleDetails(@PathVariable Long vehicleId) {
        return vehicleService.getVehicleDetails(vehicleId);
    }
}
