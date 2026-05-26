package com.lencode.paper.behavior.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.LinkedHashSet;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

class RecentViewServiceTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ZSetOperations<String, String> zSetOperations = mock(ZSetOperations.class);
    private final RecentViewService service = new RecentViewService(redisTemplate, 2);

    @Test
    void recordsRecentViewAndTrimsOldItems() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.zCard("behavior:recent:7")).thenReturn(3L);

        service.recordView(7L, 9L);

        verify(zSetOperations).add(anyString(), anyString(), anyDouble());
        verify(zSetOperations).removeRange("behavior:recent:7", 0, 0);
    }

    @Test
    void listsRecentPaperIdsInRedisOrder() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRange("behavior:recent:7", 10, 19))
                .thenReturn(new LinkedHashSet<>(Arrays.asList("9", "bad", "3")));

        assertThat(service.listRecentPaperIds(7L, 2, 10)).containsExactly(9L, 3L);
    }

    @Test
    void returnsEmptyWhenRedisFails() {
        when(redisTemplate.opsForZSet()).thenThrow(new IllegalStateException("redis down"));

        assertThatCode(() -> service.recordView(7L, 9L)).doesNotThrowAnyException();
        assertThat(service.listRecentPaperIds(7L, 1, 10)).isEmpty();
        assertThat(service.countRecentViews(7L)).isZero();
    }
}
