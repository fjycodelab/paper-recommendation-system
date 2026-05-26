package com.lencode.paper.paper.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lencode.paper.common.exception.BadRequestException;
import com.lencode.paper.auth.vo.UserResponse;
import com.lencode.paper.common.exception.NotFoundException;
import com.lencode.paper.paper.dto.CreatePaperRequest;
import com.lencode.paper.paper.dto.PaperSearchRequest;
import com.lencode.paper.paper.entity.Paper;
import com.lencode.paper.paper.mapper.PaperMapper;
import com.lencode.paper.paper.mapper.PaperTagMapper;
import com.lencode.paper.paper.vo.PaperPageResponse;
import com.lencode.paper.paper.vo.PaperResponse;
import com.lencode.paper.tag.entity.ResearchTag;
import com.lencode.paper.tag.mapper.ResearchTagMapper;
import com.lencode.paper.tag.vo.TagResponse;

@Service
public class PaperService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int SECOND_LEVEL = 2;
    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final String DELETED_STATUS = "DELETED";

    private final PaperMapper paperMapper;
    private final PaperTagMapper paperTagMapper;
    private final ResearchTagMapper tagMapper;

    public PaperService(
            PaperMapper paperMapper,
            PaperTagMapper paperTagMapper,
            ResearchTagMapper tagMapper) {
        this.paperMapper = paperMapper;
        this.paperTagMapper = paperTagMapper;
        this.tagMapper = tagMapper;
    }

    @Transactional
    public PaperResponse create(CreatePaperRequest request, UserResponse submitter) {
        if (request == null) {
            throw new BadRequestException("请求参数不能为空");
        }
        if (submitter == null || submitter.getId() == null) {
            throw new BadRequestException("提交用户不能为空");
        }

        normalize(request);
        validate(request);
        List<Long> tagIds = normalizeTagIds(request.getTagIds());
        validateTagIds(tagIds);

        try {
            Paper paper = toPaper(request, submitter.getId(), parsePublishedAt(request.getPublishedAt()));
            paperMapper.insert(paper);
            for (Long tagId : tagIds) {
                paperTagMapper.insertTag(paper.getId(), tagId);
            }
            Paper inserted = paperMapper.selectActiveById(paper.getId());
            return PaperResponse.from(inserted == null ? paper : inserted, loadTagResponses(paper.getId()));
        } catch (DuplicateKeyException ex) {
            throw new BadRequestException("论文已存在");
        }
    }

    public PaperPageResponse list(Integer page, Integer pageSize) {
        return list(page, pageSize, null);
    }

    public PaperPageResponse list(Integer page, Integer pageSize, PaperSearchRequest search) {
        int safePage = page == null ? DEFAULT_PAGE : page;
        int safePageSize = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
        validatePage(safePage, safePageSize);

        PaperSearchRequest normalizedSearch = normalizeSearch(search);
        int offset = (safePage - 1) * safePageSize;
        List<PaperResponse> items = paperMapper.selectActivePage(normalizedSearch, safePageSize, offset)
                .stream()
                .map(paper -> PaperResponse.from(paper, loadTagResponses(paper.getId())))
                .collect(Collectors.toList());
        Long total = paperMapper.countActive(normalizedSearch);
        return new PaperPageResponse(items, total == null ? 0L : total, safePage, safePageSize);
    }

    public PaperResponse get(Long id) {
        if (id == null) {
            throw new BadRequestException("论文 id 不能为空");
        }
        Paper paper = paperMapper.selectActiveById(id);
        if (paper == null) {
            throw new NotFoundException("论文不存在");
        }
        return PaperResponse.from(paper, loadTagResponses(paper.getId()));
    }

    @Transactional
    public PaperResponse update(Long id, CreatePaperRequest request) {
        if (id == null) {
            throw new BadRequestException("论文 id 不能为空");
        }
        if (request == null) {
            throw new BadRequestException("请求参数不能为空");
        }
        Paper paper = paperMapper.selectById(id);
        if (paper == null) {
            throw new NotFoundException("论文不存在");
        }

        normalize(request);
        validate(request);
        List<Long> tagIds = normalizeTagIds(request.getTagIds());
        validateTagIds(tagIds);
        applyPaperFields(paper, request, parsePublishedAt(request.getPublishedAt()));

        try {
            paperMapper.updateById(paper);
            paperTagMapper.deleteByPaperId(id);
            for (Long tagId : tagIds) {
                paperTagMapper.insertTag(id, tagId);
            }
            Paper updated = paperMapper.selectById(id);
            return PaperResponse.from(updated == null ? paper : updated, loadTagResponses(id));
        } catch (DuplicateKeyException ex) {
            throw new BadRequestException("论文已存在");
        }
    }

    @Transactional
    public PaperResponse softDelete(Long id) {
        if (id == null) {
            throw new BadRequestException("论文 id 不能为空");
        }
        Paper paper = paperMapper.selectById(id);
        if (paper == null) {
            throw new NotFoundException("论文不存在");
        }
        if (!DELETED_STATUS.equals(paper.getStatus())) {
            paperMapper.softDeleteById(id);
        }
        Paper deleted = paperMapper.selectById(id);
        return PaperResponse.from(deleted == null ? paper : deleted, loadTagResponses(id));
    }

    @Transactional
    public PaperResponse restore(Long id) {
        if (id == null) {
            throw new BadRequestException("论文 id 不能为空");
        }
        Paper paper = paperMapper.selectById(id);
        if (paper == null) {
            throw new NotFoundException("论文不存在");
        }
        paperMapper.restoreById(id);
        Paper restored = paperMapper.selectActiveById(id);
        return PaperResponse.from(restored == null ? paper : restored, loadTagResponses(id));
    }

    public PaperPageResponse listDeleted(Integer page, Integer pageSize) {
        int safePage = page == null ? DEFAULT_PAGE : page;
        int safePageSize = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
        validatePage(safePage, safePageSize);
        int offset = (safePage - 1) * safePageSize;
        List<PaperResponse> items = paperMapper.selectDeletedPage(safePageSize, offset)
                .stream()
                .map(paper -> PaperResponse.from(paper, loadTagResponses(paper.getId())))
                .collect(Collectors.toList());
        Long total = paperMapper.countDeleted();
        return new PaperPageResponse(items, total == null ? 0L : total, safePage, safePageSize);
    }

    private static void normalize(CreatePaperRequest request) {
        request.setTitle(trimToNull(request.getTitle()));
        request.setAuthors(trimToNull(request.getAuthors()));
        request.setAbstractText(trimToNull(request.getAbstractText()));
        request.setSource(trimToNull(request.getSource()));
        request.setSourcePaperId(trimToNull(request.getSourcePaperId()));
        request.setDoi(trimToNull(request.getDoi()));
        request.setSourceUrl(trimToNull(request.getSourceUrl()));
        request.setDownloadUrl(trimToNull(request.getDownloadUrl()));
        request.setKeywords(trimToNull(request.getKeywords()));
        request.setPublishedAt(trimToNull(request.getPublishedAt()));
    }

    private static void validate(CreatePaperRequest request) {
        requireMaxLength(request.getTitle(), 512, "标题过长");
        requireMaxLength(request.getSource(), 32, "来源过长");
        requireMaxLength(request.getSourcePaperId(), 128, "来源论文 id 过长");
        requireMaxLength(request.getDoi(), 191, "DOI 过长");
        requireMaxLength(request.getSourceUrl(), 1024, "URL 过长");
        requireMaxLength(request.getDownloadUrl(), 1024, "下载链接过长");
        requireMaxLength(request.getKeywords(), 1000, "关键词过长");
        if (request.getPublishYear() != null && (request.getPublishYear() < 0 || request.getPublishYear() > 3000)) {
            throw new BadRequestException("年份不合法");
        }
        if (request.getCitationCount() != null && request.getCitationCount() < 0) {
            throw new BadRequestException("引用量不能为负数");
        }
        parsePublishedAt(request.getPublishedAt());
    }

    private void validateTagIds(List<Long> tagIds) {
        for (Long tagId : tagIds) {
            ResearchTag tag = tagMapper.selectById(tagId);
            if (tag == null) {
                throw new BadRequestException("标签不存在或已禁用");
            }
            if (tag.getLevel() != SECOND_LEVEL || !ACTIVE_STATUS.equals(tag.getStatus())) {
                throw new BadRequestException("标签不存在或已禁用");
            }
        }
    }

    private List<TagResponse> loadTagResponses(Long paperId) {
        return paperTagMapper.selectTagsByPaperId(paperId)
                .stream()
                .map(TagResponse::from)
                .collect(Collectors.toList());
    }

    private static void validatePage(int page, int pageSize) {
        if (page < 1) {
            throw new BadRequestException("页码必须大于等于 1");
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new BadRequestException("每页数量必须在 1 到 100 之间");
        }
    }

    private static Paper toPaper(CreatePaperRequest request, Long submittedBy, LocalDateTime publishedAt) {
        Paper paper = new Paper();
        applyPaperFields(paper, request, publishedAt);
        paper.setStatus(ACTIVE_STATUS);
        paper.setSubmittedBy(submittedBy);
        return paper;
    }

    private static void applyPaperFields(Paper paper, CreatePaperRequest request, LocalDateTime publishedAt) {
        paper.setTitle(request.getTitle());
        paper.setAuthors(request.getAuthors());
        paper.setAbstractText(request.getAbstractText());
        paper.setPublishYear(request.getPublishYear());
        paper.setSource(request.getSource());
        paper.setSourcePaperId(request.getSourcePaperId());
        paper.setDoi(request.getDoi());
        paper.setSourceUrl(request.getSourceUrl());
        paper.setDownloadUrl(request.getDownloadUrl());
        paper.setKeywords(request.getKeywords());
        paper.setCitationCount(request.getCitationCount());
        paper.setPublishedAt(publishedAt);
    }

    private static List<Long> normalizeTagIds(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return Collections.emptyList();
        }
        return tagIds.stream()
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
    }

    private static PaperSearchRequest normalizeSearch(PaperSearchRequest search) {
        if (search == null) {
            return null;
        }
        return new PaperSearchRequest(
                trimToNull(search.getTitle()),
                trimToNull(search.getAuthor()),
                search.getYear(),
                trimToNull(search.getSource()),
                search.getTagId(),
                trimToNull(search.getAbstractKeyword())
        );
    }

    private static LocalDateTime parsePublishedAt(String value) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ex) {
            throw new BadRequestException("发布时间格式不合法");
        }
    }

    private static void requireMaxLength(String value, int maxLength, String message) {
        if (value != null && value.length() > maxLength) {
            throw new BadRequestException(message);
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

