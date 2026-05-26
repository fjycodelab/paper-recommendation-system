package com.lencode.paper.behavior.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.SettableListenableFuture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lencode.paper.behavior.dto.BehaviorEventMessage;

class BehaviorEventProducerTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BehaviorEventProducer producer = new BehaviorEventProducer(
            kafkaTemplate,
            objectMapper,
            "paper.behavior.events"
    );

    @Test
    void createsMessageWithEventIdAndOccurredAt() {
        BehaviorEventMessage message = producer.newMessage(
                7L,
                9L,
                "PAPER_DETAIL_VIEW",
                null,
                null,
                null,
                null,
                null
        );

        assertThat(message.getEventId()).isNotBlank();
        assertThat(message.getUserId()).isEqualTo(7L);
        assertThat(message.getPaperId()).isEqualTo(9L);
        assertThat(message.getOccurredAt()).isNotBlank();
    }

    @Test
    void sendsMessageToKafkaWithUserIdKey() {
        SettableListenableFuture<SendResult<String, String>> future = new SettableListenableFuture<>();
        when(kafkaTemplate.send(eq("paper.behavior.events"), eq("7"), anyString())).thenReturn(future);
        BehaviorEventMessage message = message();

        producer.send(message);
        future.set(new SendResult<>(
                new ProducerRecord<>("paper.behavior.events", "7", "payload"),
                null
        ));

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("paper.behavior.events"), eq("7"), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).contains("\"eventId\":\"evt-1\"");
        assertThat(payloadCaptor.getValue()).contains("\"eventType\":\"PAPER_SEARCH\"");
    }

    @Test
    void rejectsMessageWithoutUserId() {
        BehaviorEventMessage message = message();
        message.setUserId(null);

        assertThatThrownBy(() -> producer.send(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("行为消息 userId 不能为空");
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    private static BehaviorEventMessage message() {
        return new BehaviorEventMessage(
                "evt-1",
                7L,
                9L,
                "PAPER_SEARCH",
                "llm",
                "Alice",
                2026,
                null,
                null,
                "2026-05-26T12:00:00"
        );
    }
}
