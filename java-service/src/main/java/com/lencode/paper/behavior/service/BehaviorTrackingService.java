package com.lencode.paper.behavior.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.lencode.paper.behavior.dto.BehaviorEventMessage;
import com.lencode.paper.behavior.dto.BehaviorEventRequest;
import com.lencode.paper.common.exception.BadRequestException;
import com.lencode.paper.paper.dto.PaperSearchRequest;

@Service
public class BehaviorTrackingService {

    public static final String PAPER_SEARCH = "PAPER_SEARCH";
    public static final String TAG_FILTER = "TAG_FILTER";
    public static final String PAPER_DETAIL_VIEW = "PAPER_DETAIL_VIEW";
    public static final String DOWNLOAD_CLICK = "DOWNLOAD_CLICK";
    public static final String EXTERNAL_URL_CLICK = "EXTERNAL_URL_CLICK";

    private static final int MAX_KEYWORD_LENGTH = 255;
    private static final int MAX_AUTHOR_LENGTH = 255;
    private static final int MAX_METADATA_LENGTH = 1000;
    private static final Logger log = LoggerFactory.getLogger(BehaviorTrackingService.class);

    private final BehaviorEventProducer eventProducer;
    private final RecentViewService recentViewService;

    public BehaviorTrackingService(BehaviorEventProducer eventProducer, RecentViewService recentViewService) {
        this.eventProducer = eventProducer;
        this.recentViewService = recentViewService;
    }

    public void recordSearch(Long userId, PaperSearchRequest search) {
        if (userId == null || search == null) {
            return;
        }
        String keyword = normalizeKeyword(search.getTitle(), search.getAbstractKeyword());
        String author = truncate(trimToNull(search.getAuthor()), MAX_AUTHOR_LENGTH);
        Integer publishYear = search.getYear();
        if (keyword == null && author == null && publishYear == null) {
            return;
        }
        sendSafely(newMessage(userId, null, PAPER_SEARCH, keyword, author, publishYear, null, null));
    }

    public void recordTagFilter(Long userId, Long tagId) {
        if (userId == null || tagId == null) {
            return;
        }
        sendSafely(newMessage(userId, null, TAG_FILTER, null, null, null, tagId, null));
    }

    public void recordPaperDetailView(Long userId, Long paperId) {
        if (userId == null || paperId == null) {
            return;
        }
        recentViewService.recordView(userId, paperId);
        sendSafely(newMessage(userId, paperId, PAPER_DETAIL_VIEW, null, null, null, null, null));
    }

    public void recordDownloadClick(Long userId, Long paperId) {
        if (userId == null || paperId == null) {
            return;
        }
        sendSafely(newMessage(userId, paperId, DOWNLOAD_CLICK, null, null, null, null, null));
    }

    public BehaviorEventMessage recordExplicit(Long userId, BehaviorEventRequest request) {
        if (userId == null) {
            throw new BadRequestException("用户不能为空");
        }
        if (request == null) {
            throw new BadRequestException("行为参数不能为空");
        }
        String eventType = trimToNull(request.getEventType());
        if (!EXTERNAL_URL_CLICK.equals(eventType)) {
            throw new BadRequestException("行为类型不支持");
        }
        if (request.getPaperId() == null) {
            throw new BadRequestException("论文 id 不能为空");
        }

        BehaviorEventMessage message = newMessage(
                userId,
                request.getPaperId(),
                eventType,
                truncate(trimToNull(request.getKeyword()), MAX_KEYWORD_LENGTH),
                truncate(trimToNull(request.getAuthor()), MAX_AUTHOR_LENGTH),
                request.getPublishYear(),
                request.getTagId(),
                truncate(trimToNull(request.getMetadata()), MAX_METADATA_LENGTH)
        );
        sendSafely(message);
        return message;
    }

    private BehaviorEventMessage newMessage(
            Long userId,
            Long paperId,
            String eventType,
            String keyword,
            String author,
            Integer publishYear,
            Long tagId,
            String metadata) {
        return eventProducer.newMessage(userId, paperId, eventType, keyword, author, publishYear, tagId, metadata);
    }

    private void sendSafely(BehaviorEventMessage message) {
        try {
            eventProducer.send(message);
        } catch (RuntimeException ex) {
            log.error("Failed to enqueue behavior event {}", message == null ? null : message.getEventId(), ex);
        }
    }

    private static String normalizeKeyword(String title, String abstractKeyword) {
        String titleKeyword = trimToNull(title);
        String abstractText = trimToNull(abstractKeyword);
        if (titleKeyword == null) {
            return truncate(abstractText, MAX_KEYWORD_LENGTH);
        }
        if (abstractText == null) {
            return truncate(titleKeyword, MAX_KEYWORD_LENGTH);
        }
        return truncate(titleKeyword + " " + abstractText, MAX_KEYWORD_LENGTH);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
