package com.lencode.paper.behavior.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("paper_behavior_events")
public class PaperBehaviorEvent {

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("event_id")
    private String eventId;
    @TableField("user_id")
    private Long userId;
    @TableField("paper_id")
    private Long paperId;
    @TableField("event_type")
    private String eventType;
    private String keyword;
    private String author;
    @TableField("publish_year")
    private Integer publishYear;
    @TableField("tag_id")
    private Long tagId;
    private String metadata;
    @TableField("occurred_at")
    private LocalDateTime occurredAt;
    @TableField("created_at")
    private LocalDateTime createdAt;

    public PaperBehaviorEvent() {
    }

    public PaperBehaviorEvent(
            Long id,
            String eventId,
            Long userId,
            Long paperId,
            String eventType,
            String keyword,
            String author,
            Integer publishYear,
            Long tagId,
            String metadata,
            LocalDateTime occurredAt,
            LocalDateTime createdAt) {
        this.id = id;
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
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
