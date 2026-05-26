package com.lencode.paper.importer.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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

@Service
public class ArxivImportService {

    private static final String ARXIV_SOURCE = "arXiv";
    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final int MAX_RESULT_LIMIT = 100;
    private static final int MAX_TITLE_LENGTH = 512;
    private static final int MAX_SOURCE_ID_LENGTH = 128;
    private static final int MAX_DOI_LENGTH = 191;
    private static final int MAX_URL_LENGTH = 1024;
    private static final int MAX_KEYWORDS_LENGTH = 1000;

    private final ArxivClient arxivClient;
    private final ArxivAtomParser atomParser;
    private final PaperMapper paperMapper;
    private final PaperTagMapper paperTagMapper;
    private final ResearchTagMapper tagMapper;
    private final String defaultQuery;
    private final int defaultMaxResults;

    public ArxivImportService(
            ArxivClient arxivClient,
            ArxivAtomParser atomParser,
            PaperMapper paperMapper,
            PaperTagMapper paperTagMapper,
            ResearchTagMapper tagMapper,
            @Value("${app.paper.arxiv.default-query}") String defaultQuery,
            @Value("${app.paper.arxiv.max-results}") int defaultMaxResults) {
        this.arxivClient = arxivClient;
        this.atomParser = atomParser;
        this.paperMapper = paperMapper;
        this.paperTagMapper = paperTagMapper;
        this.tagMapper = tagMapper;
        this.defaultQuery = defaultQuery;
        this.defaultMaxResults = defaultMaxResults;
    }

    public ArxivImportResponse importPapers(ArxivImportRequest request, UserResponse importer) {
        if (importer == null || importer.getId() == null) {
            throw new BadRequestException("导入用户不能为空");
        }
        String query = normalizeQuery(request == null ? null : request.getQuery());
        int maxResults = normalizeMaxResults(request == null ? null : request.getMaxResults());
        String atomXml;
        try {
            atomXml = arxivClient.fetch(query, maxResults);
        } catch (RestClientException ex) {
            return new ArxivImportResponse(maxResults, 0, 0, 1, "arXiv 导入失败: " + shortMessage(ex));
        }
        return importEntries(maxResults, atomParser.parse(atomXml), importer.getId());
    }

    @Transactional
    ArxivImportResponse importEntries(int requested, List<ArxivPaperEntry> entries, Long importerId) {
        int imported = 0;
        int skipped = 0;
        int failed = 0;
        for (ArxivPaperEntry entry : entries) {
            ImportResult result = importOne(entry, importerId);
            if (result == ImportResult.IMPORTED) {
                imported++;
            } else if (result == ImportResult.SKIPPED) {
                skipped++;
            } else {
                failed++;
            }
        }
        String message = "arXiv 返回 " + entries.size() + " 条，导入 " + imported
                + " 条，跳过 " + skipped + " 条，失败 " + failed + " 条";
        return new ArxivImportResponse(requested, imported, skipped, failed, message);
    }

    private ImportResult importOne(ArxivPaperEntry entry, Long importerId) {
        if (entry == null || entry.getSourcePaperId() == null) {
            return ImportResult.FAILED;
        }
        if (exists(entry.getSourcePaperId())) {
            return ImportResult.SKIPPED;
        }
        try {
            Paper paper = toPaper(entry, importerId);
            paperMapper.insert(paper);
            bindTags(paper.getId(), entry.getCategories());
            return ImportResult.IMPORTED;
        } catch (DuplicateKeyException ex) {
            return ImportResult.SKIPPED;
        } catch (RuntimeException ex) {
            return ImportResult.FAILED;
        }
    }

    private boolean exists(String sourcePaperId) {
        Paper existing = paperMapper.selectOne(new QueryWrapper<Paper>()
                .eq("source", ARXIV_SOURCE)
                .eq("source_paper_id", sourcePaperId)
                .last("LIMIT 1"));
        return existing != null;
    }

    private void bindTags(Long paperId, List<String> categories) {
        for (ResearchTag tag : activeSecondLevelTags(categories)) {
            paperTagMapper.insertTag(paperId, tag.getId());
        }
    }

    private List<ResearchTag> activeSecondLevelTags(List<String> categories) {
        List<String> codes = tagCodes(categories);
        if (codes.isEmpty()) {
            return new ArrayList<>();
        }
        return tagMapper.selectList(new QueryWrapper<ResearchTag>()
                .in("code", codes)
                .eq("level", 2)
                .eq("status", ACTIVE_STATUS));
    }

    private static Paper toPaper(ArxivPaperEntry entry, Long importerId) {
        Paper paper = new Paper();
        paper.setTitle(truncate(entry.getTitle(), MAX_TITLE_LENGTH));
        paper.setAuthors(truncate(join(entry.getAuthors()), MAX_KEYWORDS_LENGTH));
        paper.setAbstractText(entry.getSummary());
        paper.setPublishYear(entry.getPublishedAt() == null ? null : entry.getPublishedAt().getYear());
        paper.setSource(ARXIV_SOURCE);
        paper.setSourcePaperId(truncate(entry.getSourcePaperId(), MAX_SOURCE_ID_LENGTH));
        paper.setDoi(truncate(entry.getDoi(), MAX_DOI_LENGTH));
        paper.setSourceUrl(truncate(entry.getSourceUrl(), MAX_URL_LENGTH));
        paper.setDownloadUrl(truncate(entry.getDownloadUrl(), MAX_URL_LENGTH));
        paper.setKeywords(truncate(join(entry.getCategories()), MAX_KEYWORDS_LENGTH));
        paper.setPublishedAt(entry.getPublishedAt());
        paper.setStatus(ACTIVE_STATUS);
        paper.setSubmittedBy(importerId);
        return paper;
    }

    private String normalizeQuery(String query) {
        String normalized = trimToNull(query);
        return normalized == null ? defaultQuery : normalized;
    }

    private int normalizeMaxResults(Integer maxResults) {
        int value = maxResults == null ? defaultMaxResults : maxResults;
        if (value < 1 || value > MAX_RESULT_LIMIT) {
            throw new BadRequestException("导入数量必须在 1 到 100 之间");
        }
        return value;
    }

    private static List<String> tagCodes(List<String> categories) {
        Set<String> codes = new LinkedHashSet<>();
        for (String category : categories == null ? new ArrayList<String>() : categories) {
            String code = categoryToTagCode(category);
            if (code != null) {
                codes.add(code);
            }
        }
        if (codes.isEmpty()) {
            codes.add("ai-machine-learning");
        }
        return new ArrayList<>(codes);
    }

    private static String categoryToTagCode(String category) {
        if ("cs.CL".equals(category)) {
            return "ai-nlp";
        }
        if ("cs.IR".equals(category)) {
            return "data-information-retrieval";
        }
        if ("cs.DB".equals(category)) {
            return "systems-database";
        }
        if ("cs.SE".equals(category)) {
            return "se-architecture";
        }
        if ("cs.HC".equals(category)) {
            return "hci-user-experience";
        }
        if ("cs.CR".equals(category) || "cs.CY".equals(category)) {
            return "systems-security";
        }
        if ("cs.AI".equals(category) || "cs.LG".equals(category) || "stat.ML".equals(category)) {
            return "ai-machine-learning";
        }
        return null;
    }

    private static String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream()
                .filter(value -> trimToNull(value) != null)
                .collect(Collectors.joining(", "));
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

    private enum ImportResult {
        IMPORTED,
        SKIPPED,
        FAILED
    }
}
