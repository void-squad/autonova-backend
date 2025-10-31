package com.autonova.auth_service.auth;

public record AuthResponse(String token, UserData user) {
    public record UserData(Long id, String email, String name) {}
}
