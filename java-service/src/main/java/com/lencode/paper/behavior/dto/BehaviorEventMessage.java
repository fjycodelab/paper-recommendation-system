package com.lencode.paper.behavior.dto;

public class BehaviorEventMessage {

    private String eventId;
    private Long userId;
    private Long paperId;
    private String eventType;
    private String keyword;
    private String author;
    private Integer publishYear;
    private Long tagId;
    private String metadata;
    private String occurredAt;

    public BehaviorEventMessage() {
    }

    public BehaviorEventMessage(
            String eventId,
            Long userId,
            Long paperId,
            String eventType,
            String keyword,
            String author,
            Integer publishYear,
            Long tagId,
            String metadata,
            String occurredAt) {
        this.eventId = eventId;
        this.userId = userId;
        this.paperId = paperId;
        this.eventType = eventType;
        this.keyword = keyword;
        this.author = author;
        this.publishYear = publishYear;
        this.tagId = tagId;
        this.metadata = metadata;
        this.occurredAt = occurredAt;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getPaperId() {
        return paperId;
    }

    public void setPaperId(Long paperId) {
        this.paperId = paperId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Integer getPublishYear() {
        return publishYear;
    }

    public void setPublishYear(Integer publishYear) {
        this.publishYear = publishYear;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(String occurredAt) {
        this.occurredAt = occurredAt;
    }
}
