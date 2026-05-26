package com.lencode.paper.download.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("paper_download_attempts")
public class PaperDownloadAttempt {

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("paper_id")
    private Long paperId;
    @TableField("requested_by")
    private Long requestedBy;
    @TableField("download_url")
    private String downloadUrl;
    private String status;
    @TableField("file_name")
    private String fileName;
    @TableField("file_size")
    private Long fileSize;
    @TableField("local_file_path")
    private String localFilePath;
    @TableField("failure_reason")
    private String failureReason;
    @TableField("created_at")
    private LocalDateTime createdAt;

    public PaperDownloadAttempt() {
    }

    public PaperDownloadAttempt(
            Long id,
            Long paperId,
            Long requestedBy,
            String downloadUrl,
            String status,
            String fileName,
            Long fileSize,
            String localFilePath,
            String failureReason,
            LocalDateTime createdAt) {
        this.id = id;
        this.paperId = paperId;
        this.requestedBy = requestedBy;
        this.downloadUrl = downloadUrl;
        this.status = status;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.localFilePath = localFilePath;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPaperId() {
        return paperId;
    }

    public void setPaperId(Long paperId) {
        this.paperId = paperId;
    }

    public Long getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(Long requestedBy) {
        this.requestedBy = requestedBy;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getLocalFilePath() {
        return localFilePath;
    }

    public void setLocalFilePath(String localFilePath) {
        this.localFilePath = localFilePath;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
