package com.lencode.paper.health;

public class ComponentHealth {

    private final String status;
    private final String detail;

    private ComponentHealth(String status, String detail) {
        this.status = status;
        this.detail = detail;
    }

    public static ComponentHealth up(String detail) {
        return new ComponentHealth("UP", detail);
    }

    public static ComponentHealth down(String detail) {
        return new ComponentHealth("DOWN", detail);
    }

    public boolean isUp() {
        return "UP".equals(status);
    }

    public String getStatus() {
        return status;
    }

    public String getDetail() {
        return detail;
    }
}

