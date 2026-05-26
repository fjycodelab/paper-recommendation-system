package com.lencode.paper.behavior.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lencode.paper.behavior.dto.BehaviorEventMessage;

@Service
public class BehaviorEventProducer {

    private static final Logger log = LoggerFactory.getLogger(BehaviorEventProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public BehaviorEventProducer(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${app.behavior.topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    public BehaviorEventMessage newMessage(
            Long userId,
            Long paperId,
            String eventType,
            String keyword,
            String author,
            Integer publishYear,
            Long tagId,
            String metadata) {
        return new BehaviorEventMessage(
                UUID.randomUUID().toString(),
                userId,
                paperId,
                eventType,
                keyword,
                author,
                publishYear,
                tagId,
                metadata,
                LocalDateTime.now().toString()
        );
    }

    public void send(BehaviorEventMessage message) {
        validate(message);
        String payload = toJson(message);
        ListenableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, messageKey(message), payload);
        future.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {
            @Override
            public void onFailure(Throwable ex) {
                // 行为日志不能阻断主流程，失败先记录，后续由调用侧决定是否降级提示。
                log.error("Failed to send behavior event {}", message.getEventId(), ex);
            }

            @Override
            public void onSuccess(SendResult<String, String> result) {
                ProducerRecord<String, String> record = result == null ? null : result.getProducerRecord();
                if (record != null) {
                    log.debug("Sent behavior event {} to topic {}", message.getEventId(), record.topic());
                }
            }
        });
    }

    private String toJson(BehaviorEventMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("行为消息序列化失败", ex);
        }
    }

    private static String messageKey(BehaviorEventMessage message) {
        if (message.getUserId() != null) {
            return String.valueOf(message.getUserId());
        }
        if (message.getPaperId() != null) {
            return String.valueOf(message.getPaperId());
        }
        return message.getEventId();
    }

    private static void validate(BehaviorEventMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("行为消息不能为空");
        }
        if (!hasText(message.getEventId())) {
            throw new IllegalArgumentException("行为消息 eventId 不能为空");
        }
        if (message.getUserId() == null) {
            throw new IllegalArgumentException("行为消息 userId 不能为空");
        }
        if (!hasText(message.getEventType())) {
            throw new IllegalArgumentException("行为消息 eventType 不能为空");
        }
        if (!hasText(message.getOccurredAt())) {
            throw new IllegalArgumentException("行为消息 occurredAt 不能为空");
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
