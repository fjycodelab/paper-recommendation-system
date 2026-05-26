package com.lencode.paper.common.response;

import java.time.Instant;

public class ApiError {

    private String message;
    private Instant timestamp;

    public ApiError() {
    }

    public ApiError(String message, Instant timestamp) {
        this.message = message;
        this.timestamp = timestamp;
    }

    public static ApiError of(String message) {
        return new ApiError(message, Instant.now());
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}

