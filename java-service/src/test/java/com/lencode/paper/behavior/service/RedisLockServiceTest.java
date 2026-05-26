package com.lencode.paper.behavior.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

class RedisLockServiceTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final RedisLockService service = new RedisLockService(redisTemplate);

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void acquiresLockWithTtl() {
        when(valueOperations.setIfAbsent("lock", "token", Duration.ofMillis(1000)))
                .thenReturn(true);

        assertThat(service.tryLock("lock", "token", 1000)).isTrue();
    }

    @Test
    void returnsFalseWhenLockAlreadyExists() {
        when(valueOperations.setIfAbsent("lock", "token", Duration.ofMillis(1000)))
                .thenReturn(false);

        assertThat(service.tryLock("lock", "token", 1000)).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void releasesLockWithCompareAndDeleteScript() {
        service.release("lock", "token");

        verify(redisTemplate).execute(
                any(DefaultRedisScript.class),
                eq(Collections.singletonList("lock")),
                eq("token")
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void ignoresReleaseFailure() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), eq("token")))
                .thenThrow(new IllegalStateException("redis down"));

        assertThatCode(() -> service.release("lock", "token")).doesNotThrowAnyException();
    }
}
