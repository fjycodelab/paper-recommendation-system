package com.lencode.paper.auth.service;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.lencode.paper.auth.dto.AuthTokenPayload;
import com.lencode.paper.auth.dto.IssuedToken;
import com.lencode.paper.auth.entity.UserAccount;
import com.lencode.paper.common.exception.UnauthorizedException;

@Service
public class AuthTokenService {

    public static final long TOKEN_TTL_SECONDS = TimeUnit.HOURS.toSeconds(24);
    private static final String TOKEN_KEY_PREFIX = "auth:token:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public AuthTokenService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public IssuedToken issueToken(UserAccount user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        Instant expiresAt = Instant.now().plusSeconds(TOKEN_TTL_SECONDS);
        AuthTokenPayload payload = new AuthTokenPayload(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                expiresAt.toString()
        );

        try {
            String tokenValue = objectMapper.writeValueAsString(payload);
            redisTemplate.opsForValue().set(tokenKey(token), tokenValue, TOKEN_TTL_SECONDS, TimeUnit.SECONDS);
            return new IssuedToken(token, expiresAt);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Token payload serialization failed", ex);
        }
    }

    public AuthTokenPayload resolve(String token) {
        if (!hasText(token)) {
            throw new UnauthorizedException("未登录");
        }

        String tokenKey = tokenKey(token);
        String tokenValue = redisTemplate.opsForValue().get(tokenKey);
        if (!hasText(tokenValue)) {
            throw new UnauthorizedException("登录已失效");
        }

        try {
            AuthTokenPayload payload = objectMapper.readValue(tokenValue, AuthTokenPayload.class);
            validatePayload(payload);
            if (Instant.parse(payload.getExpiresAt()).isBefore(Instant.now())) {
                redisTemplate.delete(tokenKey);
                throw new UnauthorizedException("登录已失效");
            }
            return payload;
        } catch (JsonProcessingException | DateTimeParseException ex) {
            redisTemplate.delete(tokenKey);
            throw new UnauthorizedException("登录状态无效");
        }
    }

    static String tokenKey(String token) {
        return TOKEN_KEY_PREFIX + token;
    }

    private static void validatePayload(AuthTokenPayload payload) {
        if (payload == null
                || payload.getUserId() == null
                || !hasText(payload.getUsername())
                || !hasText(payload.getRole())
                || !hasText(payload.getExpiresAt())) {
            throw new UnauthorizedException("登录状态无效");
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}

