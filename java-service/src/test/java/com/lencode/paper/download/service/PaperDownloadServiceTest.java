package com.lencode.paper.download.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;

import com.lencode.paper.auth.vo.UserResponse;
import com.lencode.paper.common.exception.NotFoundException;
import com.lencode.paper.download.dto.DownloadedResource;
import com.lencode.paper.download.entity.PaperDownloadAttempt;
import com.lencode.paper.download.mapper.PaperDownloadAttemptMapper;
import com.lencode.paper.download.vo.DownloadAttemptResponse;
import com.lencode.paper.paper.entity.Paper;
import com.lencode.paper.paper.mapper.PaperMapper;

class PaperDownloadServiceTest {

    private final PaperMapper paperMapper = mock(PaperMapper.class);
    private final PaperDownloadAttemptMapper attemptMapper = mock(PaperDownloadAttemptMapper.class);
    private final PaperDownloadClient downloadClient = mock(PaperDownloadClient.class);

    @TempDir
    Path downloadDir;

    @Test
    void recordsNoUrlWithoutCallingDownloader() {
        PaperDownloadService service = service();
        when(paperMapper.selectActiveById(1L)).thenReturn(paper(1L, null));
        stubAttemptInsert(10L);

        DownloadAttemptResponse response = service.attempt(1L, user());

        assertThat(response.getStatus()).isEqualTo("NO_URL");
        assertThat(response.getFailureReason()).isEqualTo("论文没有下载链接");
        assertThat(response.getExternalUrl()).isNull();
        verify(downloadClient, never()).download(any(String.class));
    }

    @Test
    void downloadsPdfAndRecordsLocalFile() throws Exception {
        PaperDownloadService service = service();
        when(paperMapper.selectActiveById(2L)).thenReturn(paper(2L, "https://example.com/paper"));
        when(downloadClient.download("https://example.com/paper"))
                .thenReturn(new DownloadedResource("%PDF-1.4".getBytes("UTF-8"), MediaType.APPLICATION_PDF, "demo.pdf"));
        stubAttemptInsert(11L);

        DownloadAttemptResponse response = service.attempt(2L, user());

        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getFileName()).endsWith("demo.pdf");
        assertThat(response.getFileSize()).isEqualTo(8L);
        assertThat(response.getExternalUrl()).isEqualTo("https://example.com/paper");
        assertThat(Files.exists(downloadDir.resolve(response.getFileName()))).isTrue();
    }

    @Test
    void recordsNonPdfWithoutSavingFile() {
        PaperDownloadService service = service();
        when(paperMapper.selectActiveById(3L)).thenReturn(paper(3L, "https://example.com/page"));
        when(downloadClient.download("https://example.com/page"))
                .thenReturn(new DownloadedResource("<html></html>".getBytes(), MediaType.TEXT_HTML, "page.html"));
        stubAttemptInsert(12L);

        DownloadAttemptResponse response = service.attempt(3L, user());

        assertThat(response.getStatus()).isEqualTo("NON_PDF");
        assertThat(response.getLocalFilePath()).isNull();
        assertThat(response.getFailureReason()).isEqualTo("下载内容不是 PDF");
    }

    @Test
    void recordsTimeoutFailure() {
        PaperDownloadService service = service();
        when(paperMapper.selectActiveById(4L)).thenReturn(paper(4L, "https://example.com/slow.pdf"));
        when(downloadClient.download("https://example.com/slow.pdf"))
                .thenThrow(new ResourceAccessException("read timed out", new SocketTimeoutException("Read timed out")));
        stubAttemptInsert(13L);

        DownloadAttemptResponse response = service.attempt(4L, user());

        assertThat(response.getStatus()).isEqualTo("TIMEOUT");
        assertThat(response.getFailureReason()).contains("read timed out");
    }

    @Test
    void recordsInvalidUrlAsFailedAttempt() {
        PaperDownloadService service = service();
        when(paperMapper.selectActiveById(5L)).thenReturn(paper(5L, "not a url"));
        stubAttemptInsert(14L);

        DownloadAttemptResponse response = service.attempt(5L, user());

        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getFailureReason()).isEqualTo("下载链接不合法");
        verify(downloadClient, never()).download(any(String.class));
    }

    @Test
    void rejectsMissingPaper() {
        PaperDownloadService service = service();
        when(paperMapper.selectActiveById(404L)).thenReturn(null);

        assertThatThrownBy(() -> service.attempt(404L, user()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("论文不存在");
        verify(attemptMapper, never()).insert(any(PaperDownloadAttempt.class));
    }

    @Test
    void storesAttemptFieldsBeforeReturning() {
        PaperDownloadService service = service();
        when(paperMapper.selectActiveById(6L)).thenReturn(paper(6L, null));
        stubAttemptInsert(15L);

        service.attempt(6L, user());

        ArgumentCaptor<PaperDownloadAttempt> captor = ArgumentCaptor.forClass(PaperDownloadAttempt.class);
        verify(attemptMapper).insert(captor.capture());
        assertThat(captor.getValue().getPaperId()).isEqualTo(6L);
        assertThat(captor.getValue().getRequestedBy()).isEqualTo(7L);
        assertThat(captor.getValue().getStatus()).isEqualTo("NO_URL");
        assertThat(captor.getValue().getCreatedAt()).isNotNull();
    }

    private PaperDownloadService service() {
        return new PaperDownloadService(
                paperMapper,
                attemptMapper,
                downloadClient,
                downloadDir.toString()
        );
    }

    private void stubAttemptInsert(Long id) {
        when(attemptMapper.insert(any(PaperDownloadAttempt.class))).thenAnswer(invocation -> {
            PaperDownloadAttempt attempt = invocation.getArgument(0);
            attempt.setId(id);
            return 1;
        });
    }

    private static UserResponse user() {
        return new UserResponse(7L, "alice", "USER", "ACTIVE");
    }

    private static Paper paper(Long id, String downloadUrl) {
        Paper paper = new Paper();
        paper.setId(id);
        paper.setDownloadUrl(downloadUrl);
        paper.setStatus("ACTIVE");
        return paper;
    }
}
