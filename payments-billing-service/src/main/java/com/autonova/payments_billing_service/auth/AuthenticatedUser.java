package com.autonova.payments_billing_service.auth;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public final class AuthenticatedUser {

    private final Long userId;
    private final String email;
    private final Set<String> roles;

    public AuthenticatedUser(Long userId, String email, Set<String> roles) {
        this.userId = Objects.requireNonNull(userId, "userId");
        this.email = Objects.requireNonNull(email, "email");
        this.roles = Collections.unmodifiableSet(new HashSet<>(roles));
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
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
