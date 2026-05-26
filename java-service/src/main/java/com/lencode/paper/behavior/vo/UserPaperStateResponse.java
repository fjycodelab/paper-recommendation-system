package com.lencode.paper.behavior.vo;

public class UserPaperStateResponse {

    private final Long paperId;
    private final boolean favorited;
    private final Integer rating;

    public UserPaperStateResponse(Long paperId, boolean favorited, Integer rating) {
        this.paperId = paperId;
        this.favorited = favorited;
        this.rating = rating;
    }

    public Long getPaperId() {
        return paperId;
    }

    public boolean isFavorited() {
        return favorited;
    }

    public Integer getRating() {
        return rating;
    }
}
