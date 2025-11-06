package com.autonova.auth_service.user.dto;

/**
 * Aggregated response for user profile requests, including optional customer data.
 */
public record UserProfileResponse(
        UserResponse user,
        CustomerProfile customer
) {
}
