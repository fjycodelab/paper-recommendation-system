package com.lencode.paper.behavior.vo;

public class BehaviorEventTypeCountResponse {

    private String eventType;
    private Long total;

    public BehaviorEventTypeCountResponse() {
    }

    public BehaviorEventTypeCountResponse(String eventType, Long total) {
        this.eventType = eventType;
        this.total = total;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }
}
