package com.lencode.paper.importer.dto;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class ArxivPaperEntry {

    private final String sourcePaperId;
    private final String title;
    private final List<String> authors;
    private final String summary;
    private final LocalDateTime publishedAt;
    private final String sourceUrl;
    private final String downloadUrl;
    private final String doi;
    private final List<String> categories;

    public ArxivPaperEntry(
            String sourcePaperId,
            String title,
            List<String> authors,
            String summary,
            LocalDateTime publishedAt,
            String sourceUrl,
            String downloadUrl,
            String doi,
            List<String> categories) {
        this.sourcePaperId = sourcePaperId;
        this.title = title;
        this.authors = authors == null ? Collections.emptyList() : authors;
        this.summary = summary;
        this.publishedAt = publishedAt;
        this.sourceUrl = sourceUrl;
        this.downloadUrl = downloadUrl;
        this.doi = doi;
        this.categories = categories == null ? Collections.emptyList() : categories;
    }

    public String getSourcePaperId() {
        return sourcePaperId;
    }

    public String getTitle() {
        return title;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public String getSummary() {
        return summary;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getDoi() {
        return doi;
    }

    public List<String> getCategories() {
        return categories;
    }
}
