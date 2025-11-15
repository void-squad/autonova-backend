package com.autonova.customer.dto;

import java.time.Instant;

/**
 * Lightweight projection used by dashboards to display vehicle totals.
 */
public record VehicleStatsResponse(long totalVehicles, Instant generatedAt) {
}
