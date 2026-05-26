package com.lencode.paper.behavior.dto;

public class RatePaperRequest {

    private Integer rating;

    public RatePaperRequest() {
    }

    public RatePaperRequest(Integer rating) {
        this.rating = rating;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }
}
