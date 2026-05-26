package com.lencode.paper.behavior.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lencode.paper.behavior.mapper.PaperBehaviorEventMapper;
import com.lencode.paper.behavior.mapper.PaperFavoriteMapper;
import com.lencode.paper.behavior.mapper.PaperRatingMapper;
import com.lencode.paper.behavior.vo.BehaviorEventTypeCountResponse;
import com.lencode.paper.behavior.vo.BehaviorStatsResponse;
import com.lencode.paper.behavior.vo.BehaviorTopPaperResponse;

class BehaviorStatsServiceTest {

    private final PaperBehaviorEventMapper eventMapper = mock(PaperBehaviorEventMapper.class);
    private final PaperFavoriteMapper favoriteMapper = mock(PaperFavoriteMapper.class);
    private final PaperRatingMapper ratingMapper = mock(PaperRatingMapper.class);
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final RedisLockService lockService = mock(RedisLockService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BehaviorStatsService service = new BehaviorStatsService(
            eventMapper,
            favoriteMapper,
            ratingMapper,
            redisTemplate,
            lockService,
            objectMapper,
            "behavior:stats:global",
            60,
            "behavior:stats:rebuild:lock",
            10000
    );

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void returnsCachedStatsWithoutRebuilding() throws Exception {
        BehaviorStatsResponse cached = BehaviorStatsResponse.ready(
                3L,
                Collections.singletonList(new BehaviorEventTypeCountResponse("PAPER_SEARCH", 2L)),
                4L,
                2L,
                4.5,
                Collections.singletonList(new BehaviorTopPaperResponse(9L, "Paper A", 2L)),
                Collections.emptyList()
        );
        when(valueOperations.get("behavior:stats:global"))
                .thenReturn(objectMapper.writeValueAsString(cached));

        BehaviorStatsResponse response = service.getGlobalStats();

        assertThat(response.isCached()).isTrue();
        assertThat(response.isRebuilding()).isFalse();
        assertThat(response.getEventTotal()).isEqualTo(3L);
        assertThat(response.getTopDetailViewedPapers()).hasSize(1);
        verifyNoInteractions(lockService, eventMapper, favoriteMapper, ratingMapper);
    }

    @Test
    void rebuildsStatsAndWritesCacheOnMiss() {
        when(valueOperations.get("behavior:stats:global")).thenReturn(null);
        when(lockService.tryLock(eq("behavior:stats:rebuild:lock"), anyString(), eq(10000L)))
                .thenReturn(true);
        when(eventMapper.countAllEvents()).thenReturn(5L);
        when(eventMapper.selectEventTypeCounts()).thenReturn(Arrays.asList(
                new BehaviorEventTypeCountResponse("PAPER_DETAIL_VIEW", 3L),
                new BehaviorEventTypeCountResponse("DOWNLOAD_CLICK", 2L)
        ));
        when(eventMapper.selectTopPapersByEventType(BehaviorTrackingService.PAPER_DETAIL_VIEW, 5))
                .thenReturn(Collections.singletonList(new BehaviorTopPaperResponse(9L, "Paper A", 3L)));
        when(eventMapper.selectTopPapersByEventType(BehaviorTrackingService.DOWNLOAD_CLICK, 5))
                .thenReturn(Collections.singletonList(new BehaviorTopPaperResponse(8L, "Paper B", 2L)));
        when(favoriteMapper.countActiveAll()).thenReturn(4L);
        when(ratingMapper.countDistinctRatingUsers()).thenReturn(2L);
        when(ratingMapper.selectAverageRating()).thenReturn(4.25);

        BehaviorStatsResponse response = service.getGlobalStats();

        assertThat(response.isCached()).isFalse();
        assertThat(response.getEventTotal()).isEqualTo(5L);
        assertThat(response.getFavoriteTotal()).isEqualTo(4L);
        assertThat(response.getRatingUserTotal()).isEqualTo(2L);
        assertThat(response.getAverageRating()).isEqualTo(4.25);
        assertThat(response.getTopDownloadClickedPapers()).extracting("paperId").containsExactly(8L);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(
                eq("behavior:stats:global"),
                payloadCaptor.capture(),
                eq(Duration.ofSeconds(60))
        );
        assertThat(payloadCaptor.getValue()).contains("\"eventTotal\":5");
        verify(lockService).release(eq("behavior:stats:rebuild:lock"), anyString());
    }

    @Test
    void avoidsDatabaseAggregationWhenLockIsOccupied() {
        when(valueOperations.get("behavior:stats:global")).thenReturn(null, null);
        when(lockService.tryLock(eq("behavior:stats:rebuild:lock"), anyString(), eq(10000L)))
                .thenReturn(false);

        BehaviorStatsResponse response = service.getGlobalStats();

        assertThat(response.isRebuilding()).isTrue();
        assertThat(response.getEventTotal()).isZero();
        verify(eventMapper, never()).countAllEvents();
        verify(lockService, never()).release(eq("behavior:stats:rebuild:lock"), anyString());
    }

    @Test
    void returnsEmptyStatsWhenDatabaseHasNoData() {
        when(valueOperations.get("behavior:stats:global")).thenReturn(null);
        when(lockService.tryLock(eq("behavior:stats:rebuild:lock"), anyString(), eq(10000L)))
                .thenReturn(true);
        when(eventMapper.countAllEvents()).thenReturn(null);
        when(eventMapper.selectEventTypeCounts()).thenReturn(null);
        when(eventMapper.selectTopPapersByEventType(anyString(), anyInt())).thenReturn(null);
        when(favoriteMapper.countActiveAll()).thenReturn(null);
        when(ratingMapper.countDistinctRatingUsers()).thenReturn(null);
        when(ratingMapper.selectAverageRating()).thenReturn(null);

        BehaviorStatsResponse response = service.getGlobalStats();

        assertThat(response.getEventTotal()).isZero();
        assertThat(response.getEventTypeCounts()).isEmpty();
        assertThat(response.getFavoriteTotal()).isZero();
        assertThat(response.getRatingUserTotal()).isZero();
        assertThat(response.getAverageRating()).isNull();
        assertThat(response.getTopDetailViewedPapers()).isEmpty();
    }
}
