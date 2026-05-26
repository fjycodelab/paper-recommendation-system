package com.lencode.paper.tag.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.lencode.paper.auth.dto.AuthenticatedUser;
import com.lencode.paper.auth.service.AuthService;
import com.lencode.paper.config.BearerTokenAuthenticationFilter;
import com.lencode.paper.config.JsonAccessDeniedHandler;
import com.lencode.paper.config.JsonAuthenticationEntryPoint;
import com.lencode.paper.config.SecurityConfig;
import com.lencode.paper.tag.dto.CreateTagRequest;
import com.lencode.paper.tag.dto.UpdateTagStatusRequest;
import com.lencode.paper.tag.service.TagService;
import com.lencode.paper.tag.vo.TagResponse;
import com.lencode.paper.tag.vo.TagTreeNodeResponse;

@WebMvcTest(TagController.class)
@Import({
        SecurityConfig.class,
        BearerTokenAuthenticationFilter.class,
        JsonAuthenticationEntryPoint.class,
        JsonAccessDeniedHandler.class
})
class TagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TagService tagService;

    @MockBean
    private AuthService authService;

    @Test
    void returnsActiveTagTree() throws Exception {
        TagTreeNodeResponse ai = new TagTreeNodeResponse(1L, null, "人工智能", 1);
        ai.getChildren().add(new TagTreeNodeResponse(2L, 1L, "推荐系统", 2));
        when(tagService.listActiveTree()).thenReturn(Arrays.asList(ai));

        mockMvc.perform(get("/api/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("人工智能"))
                .andExpect(jsonPath("$[0].children[0].name").value("推荐系统"));
    }

    @Test
    void createsTagAfterAdminCheck() throws Exception {
        when(authService.authenticateToken("token-admin"))
                .thenReturn(new AuthenticatedUser(1L, "fjy", "ADMIN", "ACTIVE"));
        when(tagService.create(any(CreateTagRequest.class)))
                .thenReturn(new TagResponse(10L, 1L, "语义检索", 2, "ACTIVE", 5));

        mockMvc.perform(post("/api/admin/tags")
                        .header("Authorization", "Bearer token-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parentId\":1,\"name\":\"语义检索\",\"sortOrder\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.parentId").value(1L))
                .andExpect(jsonPath("$.name").value("语义检索"))
                .andExpect(jsonPath("$.level").value(2))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(authService).authenticateToken("token-admin");
    }

    @Test
    void rejectsUserTokenForAdminCreate() throws Exception {
        when(authService.authenticateToken("token-user"))
                .thenReturn(new AuthenticatedUser(2L, "alice", "USER", "ACTIVE"));

        mockMvc.perform(post("/api/admin/tags")
                        .header("Authorization", "Bearer token-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"新标签\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("无权限"));
    }

    @Test
    void updatesStatusAfterAdminCheck() throws Exception {
        when(authService.authenticateToken("token-admin"))
                .thenReturn(new AuthenticatedUser(1L, "fjy", "ADMIN", "ACTIVE"));
        when(tagService.updateStatus(any(Long.class), any(UpdateTagStatusRequest.class)))
                .thenReturn(new TagResponse(10L, 1L, "语义检索", 2, "DISABLED", 5));

        mockMvc.perform(patch("/api/admin/tags/10/status")
                        .header("Authorization", "Bearer token-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.status").value("DISABLED"));

        verify(authService).authenticateToken("token-admin");
    }
}

