package com.lencode.paper.auth.dto;

import java.time.Instant;

public class IssuedToken {

    private final String token;
    private final Instant expiresAt;

    public IssuedToken(String token, Instant expiresAt) {
        this.token = token;
        this.expiresAt = expiresAt;
    }

    public String getToken() {
        return token;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}

