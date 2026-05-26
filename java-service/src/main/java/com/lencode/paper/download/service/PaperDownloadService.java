package com.lencode.paper.download.service;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import com.lencode.paper.auth.vo.UserResponse;
import com.lencode.paper.common.exception.BadRequestException;
import com.lencode.paper.common.exception.NotFoundException;
import com.lencode.paper.download.dto.DownloadStatus;
import com.lencode.paper.download.dto.DownloadedResource;
import com.lencode.paper.download.entity.PaperDownloadAttempt;
import com.lencode.paper.download.mapper.PaperDownloadAttemptMapper;
import com.lencode.paper.download.vo.DownloadAttemptResponse;
import com.lencode.paper.paper.entity.Paper;
import com.lencode.paper.paper.mapper.PaperMapper;

@Service
public class PaperDownloadService {

    private static final int MAX_FILE_NAME_LENGTH = 255;
    private static final int MAX_FAILURE_REASON_LENGTH = 1000;

    private final PaperMapper paperMapper;
    private final PaperDownloadAttemptMapper attemptMapper;
    private final PaperDownloadClient downloadClient;
    private final Path downloadRoot;

    public PaperDownloadService(
            PaperMapper paperMapper,
            PaperDownloadAttemptMapper attemptMapper,
            PaperDownloadClient downloadClient,
            @Value("${app.paper.download-dir}") String downloadDir) {
        this.paperMapper = paperMapper;
        this.attemptMapper = attemptMapper;
        this.downloadClient = downloadClient;
        this.downloadRoot = Paths.get(downloadDir).toAbsolutePath().normalize();
    }

    @Transactional
    public DownloadAttemptResponse attempt(Long paperId, UserResponse requester) {
        if (paperId == null) {
            throw new BadRequestException("论文 id 不能为空");
        }
        if (requester == null || requester.getId() == null) {
            throw new BadRequestException("请求用户不能为空");
        }

        Paper paper = paperMapper.selectActiveById(paperId);
        if (paper == null) {
            throw new NotFoundException("论文不存在");
        }

        String downloadUrl = trimToNull(paper.getDownloadUrl());
        if (downloadUrl == null) {
            return record(paperId, requester.getId(), null, DownloadStatus.NO_URL, null, null, null, "论文没有下载链接");
        }

        URI uri = parseHttpUri(downloadUrl);
        if (uri == null) {
            return record(paperId, requester.getId(), downloadUrl, DownloadStatus.FAILED, null, null, null, "下载链接不合法");
        }

        return downloadAndRecord(paperId, requester.getId(), uri);
    }

    private DownloadAttemptResponse downloadAndRecord(Long paperId, Long requesterId, URI uri) {
        try {
            DownloadedResource resource = downloadClient.download(uri.toString());
            if (!isPdf(resource)) {
                return record(paperId, requesterId, uri.toString(), DownloadStatus.NON_PDF,
                        null, null, null, "下载内容不是 PDF");
            }
            SavedFile savedFile = savePdf(paperId, uri, resource);
            return record(paperId, requesterId, uri.toString(), DownloadStatus.SUCCESS,
                    savedFile.fileName, savedFile.fileSize, savedFile.localFilePath, null);
        } catch (ResourceAccessException ex) {
            DownloadStatus status = isTimeout(ex) ? DownloadStatus.TIMEOUT : DownloadStatus.FAILED;
            return record(paperId, requesterId, uri.toString(), status, null, null, null, shortMessage(ex));
        } catch (RestClientException ex) {
            return record(paperId, requesterId, uri.toString(), DownloadStatus.FAILED,
                    null, null, null, shortMessage(ex));
        } catch (IOException ex) {
            return record(paperId, requesterId, uri.toString(), DownloadStatus.FAILED,
                    null, null, null, "保存文件失败: " + shortMessage(ex));
        }
    }

    private SavedFile savePdf(Long paperId, URI uri, DownloadedResource resource) throws IOException {
        Files.createDirectories(downloadRoot);
        String fileName = buildFileName(paperId, uri, resource.getFileName());
        Path target = downloadRoot.resolve(fileName).normalize();
        Files.write(target, resource.getBody());
        return new SavedFile(fileName, (long) resource.getBody().length, target.toString());
    }

    private DownloadAttemptResponse record(
            Long paperId,
            Long requesterId,
            String downloadUrl,
            DownloadStatus status,
            String fileName,
            Long fileSize,
            String localFilePath,
            String failureReason) {
        PaperDownloadAttempt attempt = new PaperDownloadAttempt();
        attempt.setPaperId(paperId);
        attempt.setRequestedBy(requesterId);
        attempt.setDownloadUrl(downloadUrl);
        attempt.setStatus(status.name());
        attempt.setFileName(fileName);
        attempt.setFileSize(fileSize);
        attempt.setLocalFilePath(localFilePath);
        attempt.setFailureReason(truncate(failureReason, MAX_FAILURE_REASON_LENGTH));
        attempt.setCreatedAt(LocalDateTime.now());
        attemptMapper.insert(attempt);
        return DownloadAttemptResponse.from(attempt);
    }

    private static URI parseHttpUri(String value) {
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            if (scheme == null || uri.getHost() == null) {
                return null;
            }
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                return null;
            }
            return uri;
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    private static boolean isPdf(DownloadedResource resource) {
        MediaType contentType = resource.getContentType();
        if (contentType != null
                && "application".equalsIgnoreCase(contentType.getType())
                && contentType.getSubtype() != null
                && contentType.getSubtype().toLowerCase().contains("pdf")) {
            return true;
        }
        byte[] body = resource.getBody();
        return body.length >= 4 && body[0] == '%' && body[1] == 'P' && body[2] == 'D' && body[3] == 'F';
    }

    private static String buildFileName(Long paperId, URI uri, String headerFileName) {
        String baseName = trimToNull(headerFileName);
        if (baseName == null) {
            String path = uri.getPath();
            int lastSlash = path == null ? -1 : path.lastIndexOf('/');
            baseName = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
        }
        baseName = trimToNull(baseName);
        if (baseName == null) {
            baseName = "paper";
        }
        baseName = sanitizeFileName(baseName);
        if (!baseName.toLowerCase().endsWith(".pdf")) {
            baseName = baseName + ".pdf";
        }
        String prefix = "paper-" + paperId + "-" + System.currentTimeMillis() + "-";
        return truncate(prefix + baseName, MAX_FILE_NAME_LENGTH);
    }

    private static String sanitizeFileName(String value) {
        String sanitized = value.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
        sanitized = sanitized.replaceAll("^\\.+", "");
        sanitized = trimToNull(sanitized);
        return sanitized == null ? "paper.pdf" : sanitized;
    }

    private static boolean isTimeout(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static String shortMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return ex.getClass().getSimpleName();
        }
        return ex.getClass().getSimpleName() + ": " + message;
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static class SavedFile {
        private final String fileName;
        private final Long fileSize;
        private final String localFilePath;

        private SavedFile(String fileName, Long fileSize, String localFilePath) {
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.localFilePath = localFilePath;
        }
    }
}
