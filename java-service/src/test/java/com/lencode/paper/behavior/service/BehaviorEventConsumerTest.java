package com.lencode.paper.behavior.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lencode.paper.behavior.dto.BehaviorEventMessage;
import com.lencode.paper.behavior.entity.PaperBehaviorEvent;
import com.lencode.paper.behavior.mapper.PaperBehaviorEventMapper;

class BehaviorEventConsumerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PaperBehaviorEventMapper eventMapper = mock(PaperBehaviorEventMapper.class);
    private final BehaviorEventConsumer consumer = new BehaviorEventConsumer(objectMapper, eventMapper);

    @Test
    void consumesMessageAndPersistsEvent() throws Exception {
        when(eventMapper.insertIgnore(any(PaperBehaviorEvent.class))).thenReturn(1);

        consumer.consume(objectMapper.writeValueAsString(message("evt-1")));

        ArgumentCaptor<PaperBehaviorEvent> eventCaptor = ArgumentCaptor.forClass(PaperBehaviorEvent.class);
        verify(eventMapper).insertIgnore(eventCaptor.capture());
        PaperBehaviorEvent event = eventCaptor.getValue();
        assertThat(event.getEventId()).isEqualTo("evt-1");
        assertThat(event.getUserId()).isEqualTo(7L);
        assertThat(event.getPaperId()).isEqualTo(9L);
        assertThat(event.getEventType()).isEqualTo("PAPER_SEARCH");
        assertThat(event.getKeyword()).isEqualTo("llm");
        assertThat(event.getAuthor()).isEqualTo("Alice");
        assertThat(event.getPublishYear()).isEqualTo(2026);
        assertThat(event.getOccurredAt()).isEqualTo(LocalDateTime.parse("2026-05-26T12:00:00"));
    }

    @Test
    void duplicateEventIdIsTreatedAsAlreadyHandled() throws Exception {
        when(eventMapper.insertIgnore(any(PaperBehaviorEvent.class))).thenReturn(0);

        assertThatCode(() -> consumer.consume(objectMapper.writeValueAsString(message("evt-1"))))
                .doesNotThrowAnyException();

        verify(eventMapper).insertIgnore(any(PaperBehaviorEvent.class));
    }

    @Test
    void rejectsInvalidJson() {
        assertThatThrownBy(() -> consumer.consume("{bad-json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("行为消息格式不合法");
    }

    @Test
    void rejectsMessageWithoutEventId() throws Exception {
        BehaviorEventMessage message = message(null);

        assertThatThrownBy(() -> consumer.consume(objectMapper.writeValueAsString(message)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("行为消息 eventId 不能为空");
    }

    private static BehaviorEventMessage message(String eventId) {
        return new BehaviorEventMessage(
                eventId,
                7L,
                9L,
                "PAPER_SEARCH",
                "llm",
                "Alice",
                2026,
                3L,
                "{\"source\":\"backend\"}",
                "2026-05-26T12:00:00"
        );
    }
}
