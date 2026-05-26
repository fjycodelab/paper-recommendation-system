package com.lencode.paper.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.lencode.paper.auth.dto.AuthenticatedUser;
import com.lencode.paper.auth.dto.AuthTokenPayload;
import com.lencode.paper.auth.dto.IssuedToken;
import com.lencode.paper.auth.dto.LoginRequest;
import com.lencode.paper.auth.dto.RegisterRequest;
import com.lencode.paper.auth.entity.UserAccount;
import com.lencode.paper.auth.mapper.UserMapper;
import com.lencode.paper.auth.vo.LoginResponse;
import com.lencode.paper.auth.vo.UserResponse;
import com.lencode.paper.common.exception.BadRequestException;
import com.lencode.paper.common.exception.DuplicateUsernameException;
import com.lencode.paper.common.exception.UnauthorizedException;

class AuthServiceTest {

    private final UserMapper userMapper = mock(UserMapper.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final AuthTokenService authTokenService = mock(AuthTokenService.class);
    private final AuthService authService = new AuthService(userMapper, passwordEncoder, authTokenService);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registersUserWithTrimmedUsernameAndBcryptPassword() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount user = invocation.getArgument(0);
            user.setId(10L);
            return 1;
        });

        UserResponse response = authService.register(new RegisterRequest("  alice  ", "pass123"));

        ArgumentCaptor<UserAccount> userCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userMapper).insert(userCaptor.capture());
        UserAccount inserted = userCaptor.getValue();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getUsername()).isEqualTo("alice");
        assertThat(response.getRole()).isEqualTo("USER");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(inserted.getUsername()).isEqualTo("alice");
        assertThat(inserted.getPasswordHash()).isNotEqualTo("pass123");
        assertThat(passwordEncoder.matches("pass123", inserted.getPasswordHash())).isTrue();
    }

    @Test
    void rejectsEmptyRequest() {
        assertThatThrownBy(() -> authService.register(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("请求参数不能为空");
    }

    @Test
    void rejectsBlankUsername() {
        assertThatThrownBy(() -> authService.register(new RegisterRequest("  ", "pass123")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("账号不能为空");
    }

    @Test
    void rejectsBlankPassword() {
        assertThatThrownBy(() -> authService.register(new RegisterRequest("alice", "  ")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("密码不能为空");
    }

    @Test
    void rejectsDuplicateUsername() {
        when(userMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("fjy", "123456")))
                .isInstanceOf(DuplicateUsernameException.class)
                .hasMessage("账号已存在");
        verify(userMapper, never()).insert(any(UserAccount.class));
    }

    @Test
    void logsInWithValidPasswordAndReturnsToken() {
        UserAccount user = user("fjy", passwordEncoder.encode("123456"), "ADMIN", "ACTIVE");
        when(userMapper.selectOne(any())).thenReturn(user);
        when(authTokenService.issueToken(user))
                .thenReturn(new IssuedToken("token-abc", Instant.parse("2026-05-25T00:00:00Z")));

        LoginResponse response = authService.login(new LoginRequest("  fjy  ", "123456"));

        assertThat(response.getToken()).isEqualTo("token-abc");
        assertThat(response.getExpiresInSeconds()).isEqualTo(AuthTokenService.TOKEN_TTL_SECONDS);
        assertThat(response.getUser().getUsername()).isEqualTo("fjy");
        assertThat(response.getUser().getRole()).isEqualTo("ADMIN");
    }

    @Test
    void rejectsWrongPassword() {
        UserAccount user = user("fjy", passwordEncoder.encode("123456"), "ADMIN", "ACTIVE");
        when(userMapper.selectOne(any())).thenReturn(user);

        assertThatThrownBy(() -> authService.login(new LoginRequest("fjy", "wrong")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("账号或密码错误");
    }

    @Test
    void rejectsInactiveUserLogin() {
        UserAccount user = user("bob", passwordEncoder.encode("123456"), "USER", "DISABLED");
        when(userMapper.selectOne(any())).thenReturn(user);

        assertThatThrownBy(() -> authService.login(new LoginRequest("bob", "123456")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("账号不可用");
    }

    @Test
    void authenticatesRedisTokenAndLoadsCurrentUser() {
        UserAccount user = user("fjy", passwordEncoder.encode("123456"), "ADMIN", "ACTIVE");
        when(authTokenService.resolve("token-abc")).thenReturn(new AuthTokenPayload(
                1L,
                "fjy",
                "ADMIN",
                "2026-05-25T00:00:00Z"
        ));
        when(userMapper.selectById(1L)).thenReturn(user);

        AuthenticatedUser authenticatedUser = authService.authenticateToken("token-abc");

        assertThat(authenticatedUser.getUsername()).isEqualTo("fjy");
        assertThat(authenticatedUser.getRole()).isEqualTo("ADMIN");
    }

    @Test
    void returnsCurrentUserFromSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(1L, "fjy", "ADMIN", "ACTIVE"),
                "token-abc",
                Collections.emptyList()
        ));

        UserResponse response = authService.currentUser();

        assertThat(response.getUsername()).isEqualTo("fjy");
        assertThat(response.getRole()).isEqualTo("ADMIN");
    }

    @Test
    void rejectsCurrentUserWithoutSecurityContext() {
        assertThatThrownBy(authService::currentUser)
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("未登录");
    }

    @Test
    void rejectsInactiveUserDuringTokenAuthentication() {
        UserAccount user = user("alice", passwordEncoder.encode("pass123"), "USER", "DISABLED");
        when(authTokenService.resolve("token-user")).thenReturn(new AuthTokenPayload(
                1L,
                "alice",
                "USER",
                "2026-05-25T00:00:00Z"
        ));
        when(userMapper.selectById(1L)).thenReturn(user);

        assertThatThrownBy(() -> authService.authenticateToken("token-user"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("账号不可用");
    }

    private static UserAccount user(String username, String passwordHash, String role, String status) {
        return new UserAccount(
                1L,
                username,
                passwordHash,
                role,
                status,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}

