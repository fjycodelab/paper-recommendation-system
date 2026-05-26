package com.lencode.paper.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.lencode.paper.auth.dto.AuthenticatedUser;
import com.lencode.paper.auth.dto.LoginRequest;
import com.lencode.paper.auth.dto.RegisterRequest;
import com.lencode.paper.auth.service.AuthService;
import com.lencode.paper.auth.service.AuthTokenService;
import com.lencode.paper.auth.vo.LoginResponse;
import com.lencode.paper.auth.vo.UserResponse;
import com.lencode.paper.common.exception.BadRequestException;
import com.lencode.paper.common.exception.DuplicateUsernameException;
import com.lencode.paper.common.exception.UnauthorizedException;
import com.lencode.paper.config.BearerTokenAuthenticationFilter;
import com.lencode.paper.config.JsonAccessDeniedHandler;
import com.lencode.paper.config.JsonAuthenticationEntryPoint;
import com.lencode.paper.config.SecurityConfig;

@WebMvcTest(AuthController.class)
@Import({
        SecurityConfig.class,
        BearerTokenAuthenticationFilter.class,
        JsonAuthenticationEntryPoint.class,
        JsonAccessDeniedHandler.class
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Test
    void returnsRegisteredUserPayload() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(new UserResponse(7L, "alice", "USER", "ACTIVE"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"pass123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7L))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void mapsBadRequestTo400() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new BadRequestException("账号不能为空"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"pass123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("账号不能为空"));
    }

    @Test
    void mapsDuplicateUsernameTo409() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateUsernameException("账号已存在"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"fjy\",\"password\":\"123456\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("账号已存在"));
    }

    @Test
    void returnsLoginPayload() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new LoginResponse(
                        "token-abc",
                        AuthTokenService.TOKEN_TTL_SECONDS,
                        new UserResponse(1L, "fjy", "ADMIN", "ACTIVE")
                ));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"fjy\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-abc"))
                .andExpect(jsonPath("$.expiresInSeconds").value(AuthTokenService.TOKEN_TTL_SECONDS))
                .andExpect(jsonPath("$.user.username").value("fjy"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    void mapsLoginFailureTo401() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new UnauthorizedException("账号或密码错误"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"fjy\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("账号或密码错误"));
    }

    @Test
    void returnsCurrentUserPayload() throws Exception {
        when(authService.authenticateToken("token-abc"))
                .thenReturn(new AuthenticatedUser(1L, "fjy", "ADMIN", "ACTIVE"));
        when(authService.currentUser())
                .thenReturn(new UserResponse(1L, "fjy", "ADMIN", "ACTIVE"));

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer token-abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("fjy"))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        verify(authService).authenticateToken("token-abc");
    }

    @Test
    void mapsMissingTokenTo401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("未登录"));
    }

    @Test
    void mapsInvalidTokenTo401() throws Exception {
        when(authService.authenticateToken("bad-token"))
                .thenThrow(new UnauthorizedException("登录已失效"));

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("登录已失效"));
    }
}

