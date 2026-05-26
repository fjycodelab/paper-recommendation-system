package com.lencode.paper.auth.vo;

public class LoginResponse {

    private String token;
    private long expiresInSeconds;
    private UserResponse user;

    public LoginResponse() {
    }

    public LoginResponse(String token, long expiresInSeconds, UserResponse user) {
        this.token = token;
        this.expiresInSeconds = expiresInSeconds;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public long getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public void setExpiresInSeconds(long expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }

    public UserResponse getUser() {
        return user;
    }

    public void setUser(UserResponse user) {
        this.user = user;
    }
}

