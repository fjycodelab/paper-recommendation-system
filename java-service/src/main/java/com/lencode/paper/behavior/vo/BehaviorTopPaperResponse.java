package com.lencode.paper.behavior.vo;

public class BehaviorTopPaperResponse {

    private Long paperId;
    private String title;
    private Long total;

    public BehaviorTopPaperResponse() {
    }

    public BehaviorTopPaperResponse(Long paperId, String title, Long total) {
        this.paperId = paperId;
        this.title = title;
        this.total = total;
    }

    public Long getPaperId() {
        return paperId;
    }

    public void setPaperId(Long paperId) {
        this.paperId = paperId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }
}
