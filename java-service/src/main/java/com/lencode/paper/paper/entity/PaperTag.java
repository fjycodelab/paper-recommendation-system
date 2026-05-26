package com.lencode.paper.paper.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("paper_tags")
public class PaperTag {

    @TableField("paper_id")
    private Long paperId;
    @TableField("tag_id")
    private Long tagId;
    @TableField("created_at")
    private LocalDateTime createdAt;

    public PaperTag() {
    }

    public PaperTag(Long paperId, Long tagId, LocalDateTime createdAt) {
        this.paperId = paperId;
        this.tagId = tagId;
        this.createdAt = createdAt;
    }

    public Long getPaperId() {
        return paperId;
    }

    public void setPaperId(Long paperId) {
        this.paperId = paperId;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
