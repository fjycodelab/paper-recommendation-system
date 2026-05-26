package com.lencode.paper.behavior.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.lencode.paper.behavior.dto.BehaviorEventMessage;
import com.lencode.paper.behavior.dto.BehaviorEventRequest;
import com.lencode.paper.common.exception.BadRequestException;
import com.lencode.paper.paper.dto.PaperSearchRequest;

class BehaviorTrackingServiceTest {

    private final BehaviorEventProducer eventProducer = mock(BehaviorEventProducer.class);
    private final RecentViewService recentViewService = mock(RecentViewService.class);
    private final BehaviorTrackingService service = new BehaviorTrackingService(eventProducer, recentViewService);

    @Test
    void recordsSearchWithKeywordAuthorAndYear() {
        stubNewMessage("evt-search");
        PaperSearchRequest search = new PaperSearchRequest(" llm ", " Alice ", 2026, "arXiv", null, " embedding ");

        service.recordSearch(7L, search);

        verify(eventProducer).newMessage(
                eq(7L),
                eq(null),
                eq(BehaviorTrackingService.PAPER_SEARCH),
                eq("llm embedding"),
                eq("Alice"),
                eq(2026),
                eq(null),
                eq(null)
        );
        verify(eventProducer).send(any(BehaviorEventMessage.class));
    }

    @Test
    void ignoresSearchWithoutTrackedFields() {
        PaperSearchRequest search = new PaperSearchRequest(null, null, null, "arXiv", 3L, null);

        service.recordSearch(7L, search);

        verify(eventProducer, never()).send(any(BehaviorEventMessage.class));
    }

    @Test
    void recordsTagFilter() {
        stubNewMessage("evt-tag");

        service.recordTagFilter(7L, 3L);

        verify(eventProducer).newMessage(7L, null, BehaviorTrackingService.TAG_FILTER, null, null, null, 3L, null);
        verify(eventProducer).send(any(BehaviorEventMessage.class));
    }

    @Test
    void recordsPaperDetailViewAndRecentView() {
        stubNewMessage("evt-detail");

        service.recordPaperDetailView(7L, 9L);

        verify(recentViewService).recordView(7L, 9L);
        verify(eventProducer).newMessage(7L, 9L, BehaviorTrackingService.PAPER_DETAIL_VIEW, null, null, null, null, null);
    }

    @Test
    void acceptsExplicitExternalUrlClick() {
        stubNewMessage("evt-external");
        BehaviorEventRequest request = new BehaviorEventRequest();
        request.setEventType(BehaviorTrackingService.EXTERNAL_URL_CLICK);
        request.setPaperId(9L);
        request.setMetadata("sourceUrl");

        BehaviorEventMessage message = service.recordExplicit(7L, request);

        assertThat(message.getEventId()).isEqualTo("evt-external");
        verify(eventProducer).newMessage(
                eq(7L),
                eq(9L),
                eq(BehaviorTrackingService.EXTERNAL_URL_CLICK),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq("sourceUrl")
        );
    }

    @Test
    void rejectsUnsupportedExplicitEventType() {
        BehaviorEventRequest request = new BehaviorEventRequest();
        request.setEventType(BehaviorTrackingService.PAPER_SEARCH);
        request.setPaperId(9L);

        assertThatThrownBy(() -> service.recordExplicit(7L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("行为类型不支持");
    }

    private void stubNewMessage(String eventId) {
        when(eventProducer.newMessage(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> new BehaviorEventMessage(
                        eventId,
                        invocation.getArgument(0),
                        invocation.getArgument(1),
                        invocation.getArgument(2),
                        invocation.getArgument(3),
                        invocation.getArgument(4),
                        invocation.getArgument(5),
                        invocation.getArgument(6),
                        invocation.getArgument(7),
                        "2026-05-26T12:00:00"
                ));
    }
}
