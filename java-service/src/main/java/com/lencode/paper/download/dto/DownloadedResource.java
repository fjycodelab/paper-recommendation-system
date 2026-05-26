package com.lencode.paper.download.dto;

import org.springframework.http.MediaType;

public class DownloadedResource {

    private final byte[] body;
    private final MediaType contentType;
    private final String fileName;

    public DownloadedResource(byte[] body, MediaType contentType, String fileName) {
        this.body = body == null ? new byte[0] : body;
        this.contentType = contentType;
        this.fileName = fileName;
    }

    public byte[] getBody() {
        return body;
    }

    public MediaType getContentType() {
        return contentType;
    }

    public String getFileName() {
        return fileName;
    }
}
