package com.lencode.paper.auth.dto;

public class AuthTokenPayload {

    private Long userId;
    private String username;
    private String role;
    private String expiresAt;

    public AuthTokenPayload() {
    }

    public AuthTokenPayload(Long userId, String username, String role, String expiresAt) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.expiresAt = expiresAt;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }
}

