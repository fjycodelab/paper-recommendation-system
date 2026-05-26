package com.lencode.paper.download.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.lencode.paper.auth.dto.AuthenticatedUser;
import com.lencode.paper.auth.service.AuthService;
import com.lencode.paper.auth.vo.UserResponse;
import com.lencode.paper.common.exception.NotFoundException;
import com.lencode.paper.config.BearerTokenAuthenticationFilter;
import com.lencode.paper.config.JsonAccessDeniedHandler;
import com.lencode.paper.config.JsonAuthenticationEntryPoint;
import com.lencode.paper.config.SecurityConfig;
import com.lencode.paper.download.service.PaperDownloadService;
import com.lencode.paper.download.vo.DownloadAttemptResponse;

@WebMvcTest(DownloadController.class)
@Import({
        SecurityConfig.class,
        BearerTokenAuthenticationFilter.class,
        JsonAuthenticationEntryPoint.class,
        JsonAccessDeniedHandler.class
})
class DownloadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaperDownloadService downloadService;

    @MockBean
    private AuthService authService;

    @Test
    void attemptsDownloadForLoggedInUser() throws Exception {
        UserResponse user = new UserResponse(7L, "alice", "USER", "ACTIVE");
        when(authService.authenticateToken("token-user"))
                .thenReturn(new AuthenticatedUser(7L, "alice", "USER", "ACTIVE"));
        when(authService.currentUser()).thenReturn(user);
        when(downloadService.attempt(eq(9L), any(UserResponse.class)))
                .thenReturn(response(1L, 9L, "SUCCESS"));

        mockMvc.perform(post("/api/papers/9/download-attempt")
                        .header("Authorization", "Bearer token-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paperId").value(9L))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.externalUrl").value("https://example.com/demo.pdf"));

        verify(downloadService).attempt(eq(9L), any(UserResponse.class));
    }

    @Test
    void rejectsDownloadAttemptWithoutLogin() throws Exception {
        mockMvc.perform(post("/api/papers/9/download-attempt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("未登录"));
    }

    @Test
    void mapsMissingPaperTo404() throws Exception {
        UserResponse user = new UserResponse(7L, "alice", "USER", "ACTIVE");
        when(authService.authenticateToken("token-user"))
                .thenReturn(new AuthenticatedUser(7L, "alice", "USER", "ACTIVE"));
        when(authService.currentUser()).thenReturn(user);
        when(downloadService.attempt(eq(404L), any(UserResponse.class)))
                .thenThrow(new NotFoundException("论文不存在"));

        mockMvc.perform(post("/api/papers/404/download-attempt")
                        .header("Authorization", "Bearer token-user"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("论文不存在"));
    }

    private static DownloadAttemptResponse response(Long id, Long paperId, String status) {
        return new DownloadAttemptResponse(
                id,
                paperId,
                status,
                "demo.pdf",
                8L,
                "D:/code/codex_2/lencode/paper-downloads/demo.pdf",
                "https://example.com/demo.pdf",
                null,
                "2026-05-25T21:30:00"
        );
    }
}
