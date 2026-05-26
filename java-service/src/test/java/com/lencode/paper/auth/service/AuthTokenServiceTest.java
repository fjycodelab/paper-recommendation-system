package com.lencode.paper.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.lencode.paper.auth.dto.AuthTokenPayload;
import com.lencode.paper.auth.dto.IssuedToken;
import com.lencode.paper.auth.entity.UserAccount;
import com.lencode.paper.common.exception.UnauthorizedException;

class AuthTokenServiceTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> valueOperations = mockValueOperations();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AuthTokenService tokenService = new AuthTokenService(redisTemplate, objectMapper);

    @Test
    void issueTokenWritesPayloadWith24HourTtl() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        UserAccount user = new UserAccount(
                1L,
                "fjy",
                "$2a$10$hash",
                "ADMIN",
                "ACTIVE",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        IssuedToken issuedToken = tokenService.issueToken(user);

        ArgumentCaptor<String> tokenValueCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(
                eq(AuthTokenService.tokenKey(issuedToken.getToken())),
                tokenValueCaptor.capture(),
                eq(AuthTokenService.TOKEN_TTL_SECONDS),
                eq(TimeUnit.SECONDS)
        );

        AuthTokenPayload payload = objectMapper.readValue(tokenValueCaptor.getValue(), AuthTokenPayload.class);
        assertThat(issuedToken.getToken()).isNotBlank();
        assertThat(payload.getUserId()).isEqualTo(1L);
        assertThat(payload.getUsername()).isEqualTo("fjy");
        assertThat(payload.getRole()).isEqualTo("ADMIN");
        assertThat(Instant.parse(payload.getExpiresAt())).isAfter(Instant.now().plusSeconds(86000));
    }

    @Test
    void resolvesValidTokenPayload() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        AuthTokenPayload payload = new AuthTokenPayload(
                1L,
                "fjy",
                "ADMIN",
                Instant.now().plusSeconds(60).toString()
        );
        when(valueOperations.get(AuthTokenService.tokenKey("token-abc")))
                .thenReturn(objectMapper.writeValueAsString(payload));

        AuthTokenPayload resolved = tokenService.resolve("token-abc");

        assertThat(resolved.getUserId()).isEqualTo(1L);
        assertThat(resolved.getUsername()).isEqualTo("fjy");
        assertThat(resolved.getRole()).isEqualTo("ADMIN");
    }

    @Test
    void rejectsMissingToken() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(AuthTokenService.tokenKey("missing"))).thenReturn(null);

        assertThatThrownBy(() -> tokenService.resolve("missing"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("登录已失效");
    }

    @Test
    void rejectsExpiredTokenAndDeletesIt() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        AuthTokenPayload payload = new AuthTokenPayload(
                1L,
                "fjy",
                "ADMIN",
                "2000-01-01T00:00:00Z"
        );
        when(valueOperations.get(AuthTokenService.tokenKey("expired")))
                .thenReturn(objectMapper.writeValueAsString(payload));

        assertThatThrownBy(() -> tokenService.resolve("expired"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("登录已失效");
        verify(redisTemplate).delete(AuthTokenService.tokenKey("expired"));
    }

    @SuppressWarnings("unchecked")
    private static ValueOperations<String, String> mockValueOperations() {
        return mock(ValueOperations.class);
    }
}

