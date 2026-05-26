package com.lencode.paper.importer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.ResourceAccessException;

import com.lencode.paper.auth.vo.UserResponse;
import com.lencode.paper.common.exception.BadRequestException;
import com.lencode.paper.importer.dto.ArxivImportRequest;
import com.lencode.paper.importer.dto.ArxivPaperEntry;
import com.lencode.paper.importer.vo.ArxivImportResponse;
import com.lencode.paper.paper.entity.Paper;
import com.lencode.paper.paper.mapper.PaperMapper;
import com.lencode.paper.paper.mapper.PaperTagMapper;
import com.lencode.paper.tag.entity.ResearchTag;
import com.lencode.paper.tag.mapper.ResearchTagMapper;

class ArxivImportServiceTest {

    private final ArxivClient arxivClient = mock(ArxivClient.class);
    private final ArxivAtomParser atomParser = mock(ArxivAtomParser.class);
    private final PaperMapper paperMapper = mock(PaperMapper.class);
    private final PaperTagMapper paperTagMapper = mock(PaperTagMapper.class);
    private final ResearchTagMapper tagMapper = mock(ResearchTagMapper.class);
    private final ArxivImportService importService = new ArxivImportService(
            arxivClient,
            atomParser,
            paperMapper,
            paperTagMapper,
            tagMapper,
            "cat:cs.IR",
            100
    );

    @Test
    void importsNewArxivPaperAndBindsMappedTags() {
        ArxivImportRequest request = new ArxivImportRequest();
        request.setQuery("cat:cs.CL");
        request.setMaxResults(2);
        when(arxivClient.fetch("cat:cs.CL", 2)).thenReturn("<feed/>");
        when(atomParser.parse("<feed/>")).thenReturn(Arrays.asList(entry("2401.00001", "cs.CL", "cs.IR")));
        when(paperMapper.selectOne(any())).thenReturn(null);
        when(paperMapper.insert(any(Paper.class))).thenAnswer(invocation -> {
            Paper paper = invocation.getArgument(0);
            paper.setId(20L);
            return 1;
        });
        when(tagMapper.selectList(any())).thenReturn(Arrays.asList(
                tag(2L, "ai-nlp"),
                tag(3L, "data-information-retrieval")
        ));

        ArxivImportResponse response = importService.importPapers(request, user());

        ArgumentCaptor<Paper> paperCaptor = ArgumentCaptor.forClass(Paper.class);
        verify(paperMapper).insert(paperCaptor.capture());
        assertThat(response.getRequested()).isEqualTo(2);
        assertThat(response.getImported()).isEqualTo(1);
        assertThat(response.getSkipped()).isZero();
        assertThat(response.getFailed()).isZero();
        assertThat(paperCaptor.getValue().getSource()).isEqualTo("arXiv");
        assertThat(paperCaptor.getValue().getSourcePaperId()).isEqualTo("2401.00001");
        assertThat(paperCaptor.getValue().getTitle()).isEqualTo("Neural Search");
        assertThat(paperCaptor.getValue().getAuthors()).isEqualTo("Alice, Bob");
        assertThat(paperCaptor.getValue().getKeywords()).isEqualTo("cs.CL, cs.IR");
        assertThat(paperCaptor.getValue().getSubmittedBy()).isEqualTo(7L);
        verify(paperTagMapper).insertTag(20L, 2L);
        verify(paperTagMapper).insertTag(20L, 3L);
    }

    @Test
    void skipsExistingArxivPaperBySourceId() {
        when(arxivClient.fetch("cat:cs.IR", 100)).thenReturn("<feed/>");
        when(atomParser.parse("<feed/>")).thenReturn(Arrays.asList(entry("2401.00001", "cs.IR")));
        when(paperMapper.selectOne(any())).thenReturn(new Paper());

        ArxivImportResponse response = importService.importPapers(null, user());

        assertThat(response.getImported()).isZero();
        assertThat(response.getSkipped()).isEqualTo(1);
        verify(paperMapper, never()).insert(any(Paper.class));
        verify(paperTagMapper, never()).insertTag(any(Long.class), any(Long.class));
    }

    @Test
    void returnsFailureResponseWhenArxivNetworkFails() {
        when(arxivClient.fetch("cat:cs.IR", 100))
                .thenThrow(new ResourceAccessException("connection refused"));

        ArxivImportResponse response = importService.importPapers(null, user());

        assertThat(response.getImported()).isZero();
        assertThat(response.getSkipped()).isZero();
        assertThat(response.getFailed()).isEqualTo(1);
        assertThat(response.getMessage()).contains("arXiv 导入失败");
        verify(atomParser, never()).parse(any(String.class));
        verify(paperMapper, never()).insert(any(Paper.class));
    }

    @Test
    void rejectsInvalidMaxResults() {
        ArxivImportRequest request = new ArxivImportRequest();
        request.setMaxResults(101);

        assertThatThrownBy(() -> importService.importPapers(request, user()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("导入数量必须在 1 到 100 之间");
    }

    @Test
    void countsInvalidEntryAsFailed() {
        when(arxivClient.fetch("cat:cs.IR", 100)).thenReturn("<feed/>");
        when(atomParser.parse("<feed/>")).thenReturn(Arrays.asList(new ArxivPaperEntry(
                null, "Missing ID", null, null, null, null, null, null, null
        )));

        ArxivImportResponse response = importService.importPapers(null, user());

        assertThat(response.getImported()).isZero();
        assertThat(response.getFailed()).isEqualTo(1);
    }

    private static ArxivPaperEntry entry(String sourcePaperId, String... categories) {
        return new ArxivPaperEntry(
                sourcePaperId,
                "Neural Search",
                Arrays.asList("Alice", "Bob"),
                "Abstract",
                LocalDateTime.parse("2026-05-20T12:30:00"),
                "http://arxiv.org/abs/" + sourcePaperId + "v1",
                "http://arxiv.org/pdf/" + sourcePaperId + "v1",
                null,
                Arrays.asList(categories)
        );
    }

    private static ResearchTag tag(Long id, String code) {
        ResearchTag tag = new ResearchTag();
        tag.setId(id);
        tag.setCode(code);
        tag.setLevel(2);
        tag.setStatus("ACTIVE");
        return tag;
    }

    private static UserResponse user() {
        return new UserResponse(7L, "admin", "ADMIN", "ACTIVE");
    }
}
