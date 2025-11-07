package com.autonova.auth_service.user.dto;

import lombok.Data;

/**
 * Request DTO for user lookup operations
 * Used to avoid exposing sensitive data (email, ID) in URL paths
 * Enterprise security best practice: Sensitive data should be in request body, not URL
 */
@Data
public class UserLookupRequest {
    private String email;
    private Long id;
}
