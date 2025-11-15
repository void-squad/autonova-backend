package com.automobileservice.time_logging_service.client.dto;

import lombok.Data;

@Data
public class UserResponse {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
}
