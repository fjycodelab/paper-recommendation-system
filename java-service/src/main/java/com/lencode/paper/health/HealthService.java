package com.lencode.paper.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;

@Service
public class HealthService {

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;
    private final RestTemplate restTemplate;
    private final String pythonBaseUrl;
    private final String pythonHealthPath;
    private final long pythonTimeoutMs;

    public HealthService(JdbcTemplate jdbcTemplate,
                         StringRedisTemplate redisTemplate,
                         RestTemplateBuilder restTemplateBuilder,
                         @Value("${app.python-infer.base-url}") String pythonBaseUrl,
                         @Value("${app.python-infer.health-path}") String pythonHealthPath,
                         @Value("${app.python-infer.timeout-ms}") long pythonTimeoutMs) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(pythonTimeoutMs))
                .setReadTimeout(Duration.ofMillis(pythonTimeoutMs))
                .build();
        this.pythonBaseUrl = pythonBaseUrl;
        this.pythonHealthPath = pythonHealthPath;
        this.pythonTimeoutMs = pythonTimeoutMs;
    }

    public HealthResponse check() {
        ComponentHealth mysql = checkMysql();
        ComponentHealth redis = checkRedis();
        ComponentHealth pythonInfer = checkPythonInfer();
        return HealthResponse.from(mysql, redis, pythonInfer);
    }

    ComponentHealth checkMysql() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (Integer.valueOf(1).equals(result)) {
                return ComponentHealth.up("SELECT 1 ok");
            }
            return ComponentHealth.down("Unexpected result from SELECT 1: " + result);
        } catch (DataAccessException ex) {
            return ComponentHealth.down(shortMessage(ex));
        }
    }

    ComponentHealth checkRedis() {
        try {
            String pong = redisTemplate.execute((RedisCallback<String>) connection -> connection.ping());
            if ("PONG".equalsIgnoreCase(pong)) {
                return ComponentHealth.up("PING ok");
            }
            return ComponentHealth.down("Unexpected PING response: " + pong);
        } catch (RedisConnectionFailureException ex) {
            return ComponentHealth.down(shortMessage(ex));
        } catch (DataAccessException ex) {
            return ComponentHealth.down(shortMessage(ex));
        }
    }

    ComponentHealth checkPythonInfer() {
        String healthUrl = UriComponentsBuilder.fromHttpUrl(pythonBaseUrl)
                .path(pythonHealthPath)
                .toUriString();
        try {
            String body = restTemplate.getForObject(healthUrl, String.class);
            if (body != null && body.contains("UP")) {
                return ComponentHealth.up("GET " + healthUrl + " ok");
            }
            return ComponentHealth.down("GET " + healthUrl + " returned unexpected body");
        } catch (RestClientException ex) {
            return ComponentHealth.down(shortMessage(ex));
        } catch (RuntimeException ex) {
            return ComponentHealth.down(shortMessage(ex));
        }
    }

    private static String shortMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return ex.getClass().getSimpleName();
        }
        return ex.getClass().getSimpleName() + ": " + message;
    }
}

