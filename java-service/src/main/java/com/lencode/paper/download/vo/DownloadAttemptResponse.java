package com.lencode.paper.download.vo;

import java.time.LocalDateTime;

import com.lencode.paper.download.entity.PaperDownloadAttempt;

public class DownloadAttemptResponse {

    private final Long id;
    private final Long paperId;
    private final String status;
    private final String fileName;
    private final Long fileSize;
    private final String localFilePath;
    private final String externalUrl;
    private final String failureReason;
    private final String createdAt;

    public DownloadAttemptResponse(
            Long id,
            Long paperId,
            String status,
            String fileName,
            Long fileSize,
            String localFilePath,
            String externalUrl,
            String failureReason,
            String createdAt) {
        this.id = id;
        this.paperId = paperId;
        this.status = status;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.localFilePath = localFilePath;
        this.externalUrl = externalUrl;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
    }

    public static DownloadAttemptResponse from(PaperDownloadAttempt attempt) {
        return new DownloadAttemptResponse(
                attempt.getId(),
                attempt.getPaperId(),
                attempt.getStatus(),
                attempt.getFileName(),
                attempt.getFileSize(),
                attempt.getLocalFilePath(),
                attempt.getDownloadUrl(),
                attempt.getFailureReason(),
                formatDateTime(attempt.getCreatedAt())
        );
    }

    public Long getId() {
        return id;
    }

    public Long getPaperId() {
        return paperId;
    }

    public String getStatus() {
        return status;
    }

    public String getFileName() {
        return fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public String getLocalFilePath() {
        return localFilePath;
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    private static String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.toString();
    }
}
