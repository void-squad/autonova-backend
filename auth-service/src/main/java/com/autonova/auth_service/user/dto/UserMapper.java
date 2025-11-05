package com.autonova.auth_service.user.dto;

import com.autonova.auth_service.user.model.User;

/**
 * Utility mapper for converting domain users to DTOs.
 */
public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(
                user.getId(),
                user.getUserName(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getContactOne(),
                user.getPassword(),
                user.getRole(),
                user.getAddress(),
                user.getContactTwo(),
                user.isEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
