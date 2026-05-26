package com.lencode.paper.paper.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("papers")
public class Paper {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String authors;
    @TableField("abstract_text")
    private String abstractText;
    @TableField("publish_year")
    private Integer publishYear;
    private String source;
    @TableField("source_paper_id")
    private String sourcePaperId;
    private String doi;
    @TableField("source_url")
    private String sourceUrl;
    @TableField("download_url")
    private String downloadUrl;
    private String keywords;
    @TableField("citation_count")
    private Integer citationCount;
    @TableField("published_at")
    private LocalDateTime publishedAt;
    private String status;
    @TableField("submitted_by")
    private Long submittedBy;
    @TableField("deleted_at")
    private LocalDateTime deletedAt;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Paper() {
    }

    public Paper(
            Long id,
            String title,
            String authors,
            String abstractText,
            Integer publishYear,
            String source,
            String sourcePaperId,
            String doi,
            String sourceUrl,
            String downloadUrl,
            String keywords,
            Integer citationCount,
            LocalDateTime publishedAt,
            String status,
            Long submittedBy,
            LocalDateTime deletedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.authors = authors;
        this.abstractText = abstractText;
        this.publishYear = publishYear;
        this.source = source;
        this.sourcePaperId = sourcePaperId;
        this.doi = doi;
        this.sourceUrl = sourceUrl;
        this.downloadUrl = downloadUrl;
        this.keywords = keywords;
        this.citationCount = citationCount;
        this.publishedAt = publishedAt;
        this.status = status;
        this.submittedBy = submittedBy;
        this.deletedAt = deletedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthors() {
        return authors;
    }

    public void setAuthors(String authors) {
        this.authors = authors;
    }

    public String getAbstractText() {
        return abstractText;
    }

    public void setAbstractText(String abstractText) {
        this.abstractText = abstractText;
    }

    public Integer getPublishYear() {
        return publishYear;
    }

    public void setPublishYear(Integer publishYear) {
        this.publishYear = publishYear;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourcePaperId() {
        return sourcePaperId;
    }

    public void setSourcePaperId(String sourcePaperId) {
        this.sourcePaperId = sourcePaperId;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public Integer getCitationCount() {
        return citationCount;
    }

    public void setCitationCount(Integer citationCount) {
        this.citationCount = citationCount;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(Long submittedBy) {
        this.submittedBy = submittedBy;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

