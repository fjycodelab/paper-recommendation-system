package com.lencode.paper.behavior.vo;

public class BehaviorAcceptedResponse {

    private final String eventId;
    private final String status;

    public BehaviorAcceptedResponse(String eventId, String status) {
        this.eventId = eventId;
        this.status = status;
    }

    public String getEventId() {
        return eventId;
    }

    public String getStatus() {
        return status;
    }
}
