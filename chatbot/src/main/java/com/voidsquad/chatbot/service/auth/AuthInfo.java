package com.voidsquad.chatbot.service.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * Internal representation of decoded authentication information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String scheme;
    /** User id extracted from claims when available */
    private Long userId;
    /** Email extracted from claims when available */
    private String email;
    /** Role or roles extracted from claims when available */
    private String role;
    /** First name (or given_name) extracted from claims when available */
    private String firstName;
    private Map<String, Object> claims;
    private boolean valid;
    private String error;
}
