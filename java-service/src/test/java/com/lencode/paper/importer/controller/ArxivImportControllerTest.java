package com.lencode.paper.importer.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
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
import com.lencode.paper.auth.service.AuthService;
import com.lencode.paper.auth.vo.UserResponse;
import com.lencode.paper.config.BearerTokenAuthenticationFilter;
import com.lencode.paper.config.JsonAccessDeniedHandler;
import com.lencode.paper.config.JsonAuthenticationEntryPoint;
import com.lencode.paper.config.SecurityConfig;
import com.lencode.paper.importer.dto.ArxivImportRequest;
import com.lencode.paper.importer.service.ArxivImportService;
import com.lencode.paper.importer.vo.ArxivImportResponse;

@WebMvcTest(ArxivImportController.class)
@Import({
        SecurityConfig.class,
        BearerTokenAuthenticationFilter.class,
        JsonAuthenticationEntryPoint.class,
        JsonAccessDeniedHandler.class
})
class ArxivImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ArxivImportService importService;

    @MockBean
    private AuthService authService;

    @Test
    void importsArxivPapersForAdmin() throws Exception {
        UserResponse admin = new UserResponse(1L, "fjy", "ADMIN", "ACTIVE");
        when(authService.authenticateToken("token-admin"))
                .thenReturn(new AuthenticatedUser(1L, "fjy", "ADMIN", "ACTIVE"));
        when(authService.currentUser()).thenReturn(admin);
        when(importService.importPapers(any(ArxivImportRequest.class), eq(admin)))
                .thenReturn(new ArxivImportResponse(100, 80, 20, 0, "ok"));

        mockMvc.perform(post("/api/admin/papers/import/arxiv")
                        .header("Authorization", "Bearer token-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"cat:cs.CL\",\"maxResults\":100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requested").value(100))
                .andExpect(jsonPath("$.imported").value(80))
                .andExpect(jsonPath("$.skipped").value(20))
                .andExpect(jsonPath("$.failed").value(0));

        verify(importService).importPapers(any(ArxivImportRequest.class), eq(admin));
    }

    @Test
    void rejectsUserTokenForAdminImport() throws Exception {
        when(authService.authenticateToken("token-user"))
                .thenReturn(new AuthenticatedUser(2L, "alice", "USER", "ACTIVE"));

        mockMvc.perform(post("/api/admin/papers/import/arxiv")
                        .header("Authorization", "Bearer token-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("无权限"));

        verifyNoInteractions(importService);
    }

    @Test
    void rejectsImportWithoutLogin() throws Exception {
        mockMvc.perform(post("/api/admin/papers/import/arxiv")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("未登录"));
    }
}
