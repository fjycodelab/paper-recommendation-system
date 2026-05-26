package com.lencode.paper.behavior.service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lencode.paper.behavior.mapper.PaperBehaviorEventMapper;
import com.lencode.paper.behavior.mapper.PaperFavoriteMapper;
import com.lencode.paper.behavior.mapper.PaperRatingMapper;
import com.lencode.paper.behavior.vo.BehaviorEventTypeCountResponse;
import com.lencode.paper.behavior.vo.BehaviorStatsResponse;
import com.lencode.paper.behavior.vo.BehaviorTopPaperResponse;

@Service
public class BehaviorStatsService {

    private static final Logger log = LoggerFactory.getLogger(BehaviorStatsService.class);
    private static final int TOP_LIMIT = 5;

    private final PaperBehaviorEventMapper eventMapper;
    private final PaperFavoriteMapper favoriteMapper;
    private final PaperRatingMapper ratingMapper;
    private final StringRedisTemplate redisTemplate;
    private final RedisLockService lockService;
    private final ObjectMapper objectMapper;
    private final String statsCacheKey;
    private final long statsCacheTtlSeconds;
    private final String statsLockKey;
    private final long statsLockTtlMs;

    public BehaviorStatsService(
            PaperBehaviorEventMapper eventMapper,
            PaperFavoriteMapper favoriteMapper,
            PaperRatingMapper ratingMapper,
            StringRedisTemplate redisTemplate,
            RedisLockService lockService,
            ObjectMapper objectMapper,
            @Value("${app.behavior.stats-cache-key}") String statsCacheKey,
            @Value("${app.behavior.stats-cache-ttl-seconds}") long statsCacheTtlSeconds,
            @Value("${app.behavior.stats-lock-key}") String statsLockKey,
            @Value("${app.behavior.stats-lock-ttl-ms}") long statsLockTtlMs) {
        this.eventMapper = eventMapper;
        this.favoriteMapper = favoriteMapper;
        this.ratingMapper = ratingMapper;
        this.redisTemplate = redisTemplate;
        this.lockService = lockService;
        this.objectMapper = objectMapper;
        this.statsCacheKey = statsCacheKey;
        this.statsCacheTtlSeconds = statsCacheTtlSeconds;
        this.statsLockKey = statsLockKey;
        this.statsLockTtlMs = statsLockTtlMs;
    }

    public BehaviorStatsResponse getGlobalStats() {
        BehaviorStatsResponse cached = readCachedStats();
        if (cached != null) {
            return cached;
        }

        String token = UUID.randomUUID().toString();
        boolean locked = tryLock(token);
        if (!locked) {
            BehaviorStatsResponse rechecked = readCachedStats();
            return rechecked == null ? BehaviorStatsResponse.rebuilding() : rechecked;
        }

        try {
            BehaviorStatsResponse stats = aggregateStats();
            writeCache(stats);
            return stats;
        } finally {
            lockService.release(statsLockKey, token);
        }
    }

    private BehaviorStatsResponse aggregateStats() {
        List<BehaviorEventTypeCountResponse> eventTypeCounts = nullToEmpty(eventMapper.selectEventTypeCounts());
        List<BehaviorTopPaperResponse> topDetailViewedPapers = nullToEmpty(
                eventMapper.selectTopPapersByEventType(BehaviorTrackingService.PAPER_DETAIL_VIEW, TOP_LIMIT)
        );
        List<BehaviorTopPaperResponse> topDownloadClickedPapers = nullToEmpty(
                eventMapper.selectTopPapersByEventType(BehaviorTrackingService.DOWNLOAD_CLICK, TOP_LIMIT)
        );

        return BehaviorStatsResponse.ready(
                zeroIfNull(eventMapper.countAllEvents()),
                eventTypeCounts,
                zeroIfNull(favoriteMapper.countActiveAll()),
                zeroIfNull(ratingMapper.countDistinctRatingUsers()),
                ratingMapper.selectAverageRating(),
                topDetailViewedPapers,
                topDownloadClickedPapers
        );
    }

    private BehaviorStatsResponse readCachedStats() {
        try {
            String payload = redisTemplate.opsForValue().get(statsCacheKey);
            if (payload == null || payload.trim().isEmpty()) {
                return null;
            }
            BehaviorStatsResponse response = objectMapper.readValue(payload, BehaviorStatsResponse.class);
            response.setCached(true);
            response.setRebuilding(false);
            return response;
        } catch (RuntimeException | JsonProcessingException ex) {
            log.warn("Failed to read behavior stats cache {}", statsCacheKey, ex);
            return null;
        }
    }

    private void writeCache(BehaviorStatsResponse stats) {
        try {
            redisTemplate.opsForValue().set(
                    statsCacheKey,
                    objectMapper.writeValueAsString(stats),
                    Duration.ofSeconds(statsCacheTtlSeconds)
            );
        } catch (RuntimeException | JsonProcessingException ex) {
            log.warn("Failed to write behavior stats cache {}", statsCacheKey, ex);
        }
    }

    private boolean tryLock(String token) {
        try {
            return lockService.tryLock(statsLockKey, token, statsLockTtlMs);
        } catch (RuntimeException ex) {
            log.warn("Failed to acquire behavior stats lock {}, fallback to direct aggregation", statsLockKey, ex);
            return true;
        }
    }

    private static long zeroIfNull(Long value) {
        return value == null ? 0L : value;
    }

    private static <T> List<T> nullToEmpty(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }
}
