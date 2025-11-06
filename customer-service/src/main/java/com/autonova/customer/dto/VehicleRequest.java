package com.autonova.customer.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VehicleRequest(
        @NotBlank(message = "Make is required")
        @Size(max = 60, message = "Make cannot exceed 60 characters")
        String make,

        @NotBlank(message = "Model is required")
        @Size(max = 60, message = "Model cannot exceed 60 characters")
        String model,

        @NotNull(message = "Year is required")
        @Min(value = 1900, message = "Year must be later than 1900")
        @Max(value = 2100, message = "Year looks incorrect")
        Integer year,

        @NotBlank(message = "VIN is required")
        @Size(min = 11, max = 17, message = "VIN must be between 11 and 17 characters")
        String vin,

        @NotBlank(message = "License plate is required")
        @Size(max = 20, message = "License plate cannot exceed 20 characters")
        @Pattern(regexp = "^[A-Z0-9-\\s]{1,20}$", message = "License plate contains invalid characters")
        String licensePlate
) {
}
