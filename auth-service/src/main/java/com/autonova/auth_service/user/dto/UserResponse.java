package com.autonova.auth_service.user.dto;

import com.autonova.auth_service.user.Role;
import java.time.Instant;

/**
 * API representation for user details.
 */
public record UserResponse(
        Long id,
        String userName,
        String firstName,
        String lastName,
        String email,
        String contactOne,
        String password,
        Role role,
        String address,
        String contactTwo,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}
