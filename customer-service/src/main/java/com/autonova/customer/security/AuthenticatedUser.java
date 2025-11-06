package com.autonova.customer.security;

import java.io.Serializable;
import java.util.Locale;

/**
 * Lightweight representation of the authenticated principal extracted from the JWT.
 */
public record AuthenticatedUser(Long userId, String email, String role) implements Serializable {

    public String normalizedEmail() {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public boolean hasRole(String expectedRole) {
        if (role == null || expectedRole == null) {
            return false;
        }
        return role.trim().equalsIgnoreCase(expectedRole.trim());
    }
}
