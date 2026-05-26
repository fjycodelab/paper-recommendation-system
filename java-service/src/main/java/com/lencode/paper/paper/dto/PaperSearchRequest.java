package com.lencode.paper.paper.dto;

public class PaperSearchRequest {

    private final String title;
    private final String author;
    private final Integer year;
    private final String source;
    private final Long tagId;
    private final String abstractKeyword;

    public PaperSearchRequest(
            String title,
            String author,
            Integer year,
            String source,
            Long tagId,
            String abstractKeyword) {
        this.title = title;
        this.author = author;
        this.year = year;
        this.source = source;
        this.tagId = tagId;
        this.abstractKeyword = abstractKeyword;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public Integer getYear() {
        return year;
    }

    public String getSource() {
        return source;
    }

    public Long getTagId() {
        return tagId;
    }

    public String getAbstractKeyword() {
        return abstractKeyword;
    }
}

