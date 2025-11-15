package com.voidsquad.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Holds decoded information from an Authorization header (e.g., Bearer JWT payload claims).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthInfoDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** The original authorization header scheme (e.g., "Bearer") */
    private String scheme;

    /** User id extracted from claims when available */
    private Long userId;
    /** Email extracted from claims when available */
    private String email;
    /** Role or roles extracted from claims when available */
    private String role;
    /** First name (or given_name) extracted from claims when available */
    private String firstName;

    /** Whether the header was parsed successfully */
    private boolean valid;

    /** Optional error message when parsing failed */
    private String error;
}
