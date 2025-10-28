package com.autonova.payments_billing_service.auth;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public final class AuthenticatedUser {

    private final UUID userId;
    private final Set<String> roles;

    public AuthenticatedUser(UUID userId, Set<String> roles) {
        this.userId = Objects.requireNonNull(userId, "userId");
        this.roles = Collections.unmodifiableSet(new HashSet<>(roles));
    }

    public UUID getUserId() {
        return userId;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public boolean hasRole(String role) {
        return roles.stream().anyMatch(r -> r.equalsIgnoreCase(role));
    }

    public Set<GrantedAuthority> asAuthorities() {
        return roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
            .collect(HashSet::new, Set::add, Set::addAll);
    }
}
