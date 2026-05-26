package com.lencode.paper.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

class HealthServiceTest {

    @Test
    void reportsRedisDownWhenRedisPingFails() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(anyRedisCallback()))
                .thenThrow(new RedisConnectionFailureException("connection refused"));

        HealthService service = new HealthService(
                jdbcTemplate,
                redisTemplate,
                new RestTemplateBuilder(),
                "http://127.0.0.1:1",
                "/health",
                50
        );

        ComponentHealth redis = service.checkRedis();

        assertThat(redis.getStatus()).isEqualTo("DOWN");
        assertThat(redis.getDetail()).contains("connection refused");
    }

    @Test
    void reportsDegradedWhenMysqlAndRedisAreUpButPythonIsDown() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        when(redisTemplate.execute(anyRedisCallback())).thenReturn("PONG");

        HealthService service = new HealthService(
                jdbcTemplate,
                redisTemplate,
                new RestTemplateBuilder(),
                "http://127.0.0.1:1",
                "/health",
                50
        );

        HealthResponse response = service.check();

        assertThat(response.getMysql()).isEqualTo("UP");
        assertThat(response.getRedis()).isEqualTo("UP");
        assertThat(response.getPythonInfer()).isEqualTo("DOWN");
        assertThat(response.getStatus()).isEqualTo("DEGRADED");
    }

    @SuppressWarnings("unchecked")
    private static RedisCallback<String> anyRedisCallback() {
        return any(RedisCallback.class);
    }
}

