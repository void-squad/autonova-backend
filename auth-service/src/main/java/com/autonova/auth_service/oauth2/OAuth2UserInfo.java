package com.autonova.auth_service.oauth2;

import lombok.Data;

import java.util.Map;

/**
 * OAuth2 User Info
 * Represents user information from OAuth2 providers (Google, etc.)
 */
@Data
public class OAuth2UserInfo {
    private String id;
    private String email;
    private String name;
    private String firstName;
    private String lastName;
    private String picture;
    private Boolean emailVerified;

    /**
     * Create OAuth2UserInfo from Google user attributes
     */
    public static OAuth2UserInfo fromGoogle(Map<String, Object> attributes) {
        OAuth2UserInfo userInfo = new OAuth2UserInfo();
        userInfo.setId((String) attributes.get("sub"));
        userInfo.setEmail((String) attributes.get("email"));
        userInfo.setName((String) attributes.get("name"));
        userInfo.setFirstName((String) attributes.get("given_name"));
        userInfo.setLastName((String) attributes.get("family_name"));
        userInfo.setPicture((String) attributes.get("picture"));
        userInfo.setEmailVerified((Boolean) attributes.get("email_verified"));
        return userInfo;
    }
}
