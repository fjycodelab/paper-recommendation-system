package com.lencode.paper.paper.vo;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import com.lencode.paper.paper.entity.Paper;
import com.lencode.paper.tag.vo.TagResponse;

public class PaperResponse {

    private final Long id;
    private final String title;
    private final String authors;
    private final String abstractText;
    private final Integer publishYear;
    private final String source;
    private final String sourcePaperId;
    private final String doi;
    private final String sourceUrl;
    private final String downloadUrl;
    private final String keywords;
    private final Integer citationCount;
    private final String publishedAt;
    private final String status;
    private final Long submittedBy;
    private final List<TagResponse> tags;
    private final boolean favorited;
    private final Integer rating;
    private final String createdAt;
    private final String updatedAt;

    public PaperResponse(
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
            String publishedAt,
            String status,
            Long submittedBy,
            String createdAt,
            String updatedAt) {
        this(
                id, title, authors, abstractText, publishYear, source, sourcePaperId,
                doi, sourceUrl, downloadUrl, keywords, citationCount, publishedAt,
                status, submittedBy, Collections.emptyList(), false, null, createdAt, updatedAt
        );
    }

    public PaperResponse(
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
            String publishedAt,
            String status,
            Long submittedBy,
            List<TagResponse> tags,
            String createdAt,
            String updatedAt) {
        this(
                id, title, authors, abstractText, publishYear, source, sourcePaperId,
                doi, sourceUrl, downloadUrl, keywords, citationCount, publishedAt,
                status, submittedBy, tags, false, null, createdAt, updatedAt
        );
    }

    private PaperResponse(
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
            String publishedAt,
            String status,
            Long submittedBy,
            List<TagResponse> tags,
            boolean favorited,
            Integer rating,
            String createdAt,
            String updatedAt) {
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
        this.tags = tags == null ? Collections.emptyList() : tags;
        this.favorited = favorited;
        this.rating = rating;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PaperResponse from(Paper paper) {
        return new PaperResponse(
                paper.getId(),
                paper.getTitle(),
                paper.getAuthors(),
                paper.getAbstractText(),
                paper.getPublishYear(),
                paper.getSource(),
                paper.getSourcePaperId(),
                paper.getDoi(),
                paper.getSourceUrl(),
                paper.getDownloadUrl(),
                paper.getKeywords(),
                paper.getCitationCount(),
                formatDateTime(paper.getPublishedAt()),
                paper.getStatus(),
                paper.getSubmittedBy(),
                formatDateTime(paper.getCreatedAt()),
                formatDateTime(paper.getUpdatedAt())
        );
    }

    public PaperResponse withUserState(boolean favorited, Integer rating) {
        return new PaperResponse(
                id,
                title,
                authors,
                abstractText,
                publishYear,
                source,
                sourcePaperId,
                doi,
                sourceUrl,
                downloadUrl,
                keywords,
                citationCount,
                publishedAt,
                status,
                submittedBy,
                tags,
                favorited,
                rating,
                createdAt,
                updatedAt
        );
    }

    public static PaperResponse from(Paper paper, List<TagResponse> tags) {
        return new PaperResponse(
                paper.getId(),
                paper.getTitle(),
                paper.getAuthors(),
                paper.getAbstractText(),
                paper.getPublishYear(),
                paper.getSource(),
                paper.getSourcePaperId(),
                paper.getDoi(),
                paper.getSourceUrl(),
                paper.getDownloadUrl(),
                paper.getKeywords(),
                paper.getCitationCount(),
                formatDateTime(paper.getPublishedAt()),
                paper.getStatus(),
                paper.getSubmittedBy(),
                tags,
                formatDateTime(paper.getCreatedAt()),
                formatDateTime(paper.getUpdatedAt())
        );
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthors() {
        return authors;
    }

    public String getAbstractText() {
        return abstractText;
    }

    public Integer getPublishYear() {
        return publishYear;
    }

    public String getSource() {
        return source;
    }

    public String getSourcePaperId() {
        return sourcePaperId;
    }

    public String getDoi() {
        return doi;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getKeywords() {
        return keywords;
    }

    public Integer getCitationCount() {
        return citationCount;
    }

    public String getPublishedAt() {
        return publishedAt;
    }

    public String getStatus() {
        return status;
    }

    public Long getSubmittedBy() {
        return submittedBy;
    }

    public List<TagResponse> getTags() {
        return tags;
    }

    public boolean isFavorited() {
        return favorited;
    }

    public Integer getRating() {
        return rating;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    private static String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.toString();
    }
}

