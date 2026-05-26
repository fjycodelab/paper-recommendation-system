package com.lencode.paper.auth.vo;

public class AdminPingResponse {

    private boolean ok;

    public AdminPingResponse() {
    }

    public AdminPingResponse(boolean ok) {
        this.ok = ok;
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }
}

