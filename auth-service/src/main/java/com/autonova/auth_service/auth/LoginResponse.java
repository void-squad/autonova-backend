package com.autonova.auth_service.auth;

public class LoginResponse {
    private String token;          // Access token (JWT) - 1 hour
    private String refreshToken;   // Refresh token - 7 days
    private String type = "Bearer";
    private UserInfo user;

    // Constructors
    public LoginResponse() {}

    public LoginResponse(String token, UserInfo user) {
        this.token = token;
        this.user = user;
    }

    public LoginResponse(String token, String refreshToken, UserInfo user) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.user = user;
    }

    // Getters and Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public UserInfo getUser() {
        return user;
    }

    public void setUser(UserInfo user) {
        this.user = user;
    }

    // Inner class for user info
    public static class UserInfo {
        private Long id;
        private String userName;
        private String email;
        private String role;

        public UserInfo() {}

        public UserInfo(Long id, String userName, String email, String role) {
            this.id = id;
            this.userName = userName;
            this.email = email;
            this.role = role;
        }

        // Getters and Setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }
}
