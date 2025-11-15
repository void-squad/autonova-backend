package com.autonova.customer.controller;

import com.autonova.customer.dto.VehicleStatsResponse;
import com.autonova.customer.service.VehicleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Provides lightweight statistics for UI dashboards (e.g. total number of vehicles).
 */
@RestController
@RequestMapping("/api/customers")
public class VehicleStatsController {

    private final VehicleService vehicleService;

    public VehicleStatsController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping("/{customerId}/vehicles/stats")
    public VehicleStatsResponse getVehicleStats(@PathVariable Long customerId) {
        return vehicleService.getVehicleStats(customerId);
    }

    @GetMapping("/me/vehicles/stats")
    public VehicleStatsResponse getVehicleStatsForCurrentCustomer() {
        return vehicleService.getVehicleStatsForCurrentCustomer();
    }
}
