package com.lencode.paper.behavior.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lencode.paper.behavior.dto.BehaviorEventMessage;
import com.lencode.paper.behavior.entity.PaperBehaviorEvent;
import com.lencode.paper.behavior.mapper.PaperBehaviorEventMapper;

@Service
public class BehaviorEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(BehaviorEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final PaperBehaviorEventMapper eventMapper;

    public BehaviorEventConsumer(ObjectMapper objectMapper, PaperBehaviorEventMapper eventMapper) {
        this.objectMapper = objectMapper;
        this.eventMapper = eventMapper;
    }

    @KafkaListener(
            topics = "${app.behavior.topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            autoStartup = "${app.behavior.consumer-auto-startup:true}")
    public void consume(String payload) {
        BehaviorEventMessage message = parse(payload);
        PaperBehaviorEvent event = toEvent(message);
        int inserted = eventMapper.insertIgnore(event);
        if (inserted == 0) {
            log.debug("Ignored duplicate behavior event {}", event.getEventId());
        }
    }

    private BehaviorEventMessage parse(String payload) {
        if (!hasText(payload)) {
            throw new IllegalArgumentException("行为消息不能为空");
        }
        try {
            return objectMapper.readValue(payload, BehaviorEventMessage.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("行为消息格式不合法", ex);
        }
    }

    private static PaperBehaviorEvent toEvent(BehaviorEventMessage message) {
        validate(message);
        PaperBehaviorEvent event = new PaperBehaviorEvent();
        event.setEventId(message.getEventId());
        event.setUserId(message.getUserId());
        event.setPaperId(message.getPaperId());
        event.setEventType(message.getEventType());
        event.setKeyword(message.getKeyword());
        event.setAuthor(message.getAuthor());
        event.setPublishYear(message.getPublishYear());
        event.setTagId(message.getTagId());
        event.setMetadata(message.getMetadata());
        event.setOccurredAt(parseOccurredAt(message.getOccurredAt()));
        return event;
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

    private static LocalDateTime parseOccurredAt(String value) {
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("行为发生时间格式不合法", ex);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
