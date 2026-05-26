package com.lencode.paper.behavior.service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RecentViewService {

    private static final Logger log = LoggerFactory.getLogger(RecentViewService.class);
    private static final String KEY_PREFIX = "behavior:recent:";

    private final StringRedisTemplate redisTemplate;
    private final int recentViewLimit;

    public RecentViewService(
            StringRedisTemplate redisTemplate,
            @Value("${app.behavior.recent-view-limit}") int recentViewLimit) {
        this.redisTemplate = redisTemplate;
        this.recentViewLimit = recentViewLimit;
    }

    public void recordView(Long userId, Long paperId) {
        if (userId == null || paperId == null) {
            return;
        }
        String key = key(userId);
        try {
            redisTemplate.opsForZSet().add(key, String.valueOf(paperId), System.currentTimeMillis());
            Long size = redisTemplate.opsForZSet().zCard(key);
            if (size != null && size > recentViewLimit) {
                redisTemplate.opsForZSet().removeRange(key, 0, size - recentViewLimit - 1);
            }
        } catch (RuntimeException ex) {
            log.warn("Failed to record recent view for user {} paper {}", userId, paperId, ex);
        }
    }

    public List<Long> listRecentPaperIds(Long userId, int page, int pageSize) {
        if (userId == null) {
            return Collections.emptyList();
        }
        long start = (long) (page - 1) * pageSize;
        long end = start + pageSize - 1L;
        try {
            Set<String> values = redisTemplate.opsForZSet().reverseRange(key(userId), start, end);
            if (values == null || values.isEmpty()) {
                return Collections.emptyList();
            }
            return values.stream()
                    .map(RecentViewService::parseLong)
                    .filter(value -> value != null)
                    .collect(Collectors.toList());
        } catch (RuntimeException ex) {
            log.warn("Failed to list recent views for user {}", userId, ex);
            return Collections.emptyList();
        }
    }

    public long countRecentViews(Long userId) {
        if (userId == null) {
            return 0L;
        }
        try {
            Long count = redisTemplate.opsForZSet().zCard(key(userId));
            return count == null ? 0L : count;
        } catch (RuntimeException ex) {
            log.warn("Failed to count recent views for user {}", userId, ex);
            return 0L;
        }
    }

    private static String key(Long userId) {
        return KEY_PREFIX + userId;
    }

    private static Long parseLong(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
