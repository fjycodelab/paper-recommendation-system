package com.lencode.paper.paper.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.lencode.paper.auth.vo.UserResponse;
import com.lencode.paper.behavior.service.BehaviorTrackingService;
import com.lencode.paper.common.exception.NotFoundException;
import com.lencode.paper.config.BearerTokenAuthenticationFilter;
import com.lencode.paper.config.JsonAccessDeniedHandler;
import com.lencode.paper.config.JsonAuthenticationEntryPoint;
import com.lencode.paper.config.SecurityConfig;
import com.lencode.paper.paper.dto.CreatePaperRequest;
import com.lencode.paper.paper.dto.PaperSearchRequest;
import com.lencode.paper.paper.service.PaperService;
import com.lencode.paper.paper.vo.PaperPageResponse;
import com.lencode.paper.paper.vo.PaperResponse;

@WebMvcTest(PaperController.class)
@Import({
        SecurityConfig.class,
        BearerTokenAuthenticationFilter.class,
        JsonAuthenticationEntryPoint.class,
        JsonAccessDeniedHandler.class
})
class PaperControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaperService paperService;

    @MockBean
    private AuthService authService;

    @MockBean
    private BehaviorTrackingService trackingService;

    @Test
    void createsPaperForLoggedInUser() throws Exception {
        UserResponse user = new UserResponse(1L, "fjy", "ADMIN", "ACTIVE");
        when(authService.authenticateToken("token-admin"))
                .thenReturn(new AuthenticatedUser(1L, "fjy", "ADMIN", "ACTIVE"));
        when(authService.currentUser()).thenReturn(user);
        when(paperService.create(any(CreatePaperRequest.class), any(UserResponse.class)))
                .thenReturn(response(9L, "A Paper", "ACTIVE"));

        mockMvc.perform(post("/api/papers")
                        .header("Authorization", "Bearer token-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"A Paper\",\"authors\":\"Alice\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9L))
                .andExpect(jsonPath("$.title").value("A Paper"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void rejectsCreateWithoutLogin() throws Exception {
        mockMvc.perform(post("/api/papers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("未登录"));
    }

    @Test
    void returnsPagedActivePapers() throws Exception {
        stubLoggedInUser("token-user", user());
        when(paperService.list(eq(2), eq(5), any(PaperSearchRequest.class), any(UserResponse.class)))
                .thenReturn(new PaperPageResponse(
                Arrays.asList(response(2L, "Second", "ACTIVE")),
                12L,
                2,
                5
        ));

        mockMvc.perform(get("/api/papers?page=2&pageSize=5")
                        .header("Authorization", "Bearer token-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(12L))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.pageSize").value(5))
                .andExpect(jsonPath("$.items[0].title").value("Second"));
    }

    @Test
    void acceptsSearchQueryParameters() throws Exception {
        stubLoggedInUser("token-user", user());
        when(paperService.list(eq(1), eq(10), any(PaperSearchRequest.class), any(UserResponse.class)))
                .thenReturn(new PaperPageResponse(Arrays.asList(response(3L, "Search Result", "ACTIVE")), 1L, 1, 10));

        mockMvc.perform(get("/api/papers")
                        .header("Authorization", "Bearer token-user")
                        .param("page", "1")
                        .param("pageSize", "10")
                        .param("title", "recommend")
                        .param("author", "Alice")
                        .param("year", "2026")
                        .param("source", "arXiv")
                        .param("tagId", "2")
                        .param("abstractKeyword", "embedding"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("Search Result"));
        verify(trackingService).recordSearch(any(Long.class), any(PaperSearchRequest.class));
        verify(trackingService).recordTagFilter(eq(2L), eq(2L));
    }

    @Test
    void returnsPaperDetail() throws Exception {
        stubLoggedInUser("token-user", user());
        when(paperService.get(eq(9L), any(UserResponse.class))).thenReturn(response(9L, "A Paper", "ACTIVE"));

        mockMvc.perform(get("/api/papers/9")
                        .header("Authorization", "Bearer token-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9L))
                .andExpect(jsonPath("$.title").value("A Paper"));
        verify(trackingService).recordPaperDetailView(eq(2L), eq(9L));
    }

    @Test
    void mapsMissingPaperTo404() throws Exception {
        stubLoggedInUser("token-user", user());
        when(paperService.get(eq(99L), any(UserResponse.class))).thenThrow(new NotFoundException("论文不存在"));

        mockMvc.perform(get("/api/papers/99")
                        .header("Authorization", "Bearer token-user"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("论文不存在"));
    }

    @Test
    void rejectsPaperListWithoutLogin() throws Exception {
        mockMvc.perform(get("/api/papers"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("未登录"));
    }

    @Test
    void returnsMyFavoritesForLoggedInUser() throws Exception {
        stubLoggedInUser("token-user", user());
        when(paperService.listFavorites(eq(1), eq(10), any(UserResponse.class)))
                .thenReturn(new PaperPageResponse(Arrays.asList(response(3L, "Favorite", "ACTIVE")), 1L, 1, 10));

        mockMvc.perform(get("/api/me/favorites?page=1&pageSize=10")
                        .header("Authorization", "Bearer token-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1L))
                .andExpect(jsonPath("$.items[0].title").value("Favorite"));
    }

    @Test
    void returnsRecentViewsForLoggedInUser() throws Exception {
        stubLoggedInUser("token-user", user());
        when(paperService.listRecentViews(eq(1), eq(10), any(UserResponse.class)))
                .thenReturn(new PaperPageResponse(Arrays.asList(response(4L, "Recent", "ACTIVE")), 1L, 1, 10));

        mockMvc.perform(get("/api/me/recent-views?page=1&pageSize=10")
                        .header("Authorization", "Bearer token-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1L))
                .andExpect(jsonPath("$.items[0].title").value("Recent"));
    }

    @Test
    void updatesPaperAfterAdminCheck() throws Exception {
        when(authService.authenticateToken("token-admin"))
                .thenReturn(new AuthenticatedUser(1L, "fjy", "ADMIN", "ACTIVE"));
        when(paperService.update(eq(9L), any(CreatePaperRequest.class)))
                .thenReturn(response(9L, "Updated", "ACTIVE"));

        mockMvc.perform(put("/api/admin/papers/9")
                        .header("Authorization", "Bearer token-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"));

        verify(authService).authenticateToken("token-admin");
    }

    @Test
    void rejectsUserTokenForAdminUpdate() throws Exception {
        when(authService.authenticateToken("token-user"))
                .thenReturn(new AuthenticatedUser(2L, "alice", "USER", "ACTIVE"));

        mockMvc.perform(put("/api/admin/papers/9")
                        .header("Authorization", "Bearer token-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("无权限"));
    }

    @Test
    void softDeletesPaperAfterAdminCheck() throws Exception {
        when(authService.authenticateToken("token-admin"))
                .thenReturn(new AuthenticatedUser(1L, "fjy", "ADMIN", "ACTIVE"));
        when(paperService.softDelete(9L)).thenReturn(response(9L, "A Paper", "DELETED"));

        mockMvc.perform(delete("/api/admin/papers/9")
                        .header("Authorization", "Bearer token-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELETED"));

        verify(authService).authenticateToken("token-admin");
    }

    @Test
    void restoresPaperAfterAdminCheck() throws Exception {
        when(authService.authenticateToken("token-admin"))
                .thenReturn(new AuthenticatedUser(1L, "fjy", "ADMIN", "ACTIVE"));
        when(paperService.restore(9L)).thenReturn(response(9L, "A Paper", "ACTIVE"));

        mockMvc.perform(post("/api/admin/papers/9/restore")
                        .header("Authorization", "Bearer token-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(authService).authenticateToken("token-admin");
    }

    @Test
    void returnsDeletedPaperPageForAdmin() throws Exception {
        when(authService.authenticateToken("token-admin"))
                .thenReturn(new AuthenticatedUser(1L, "fjy", "ADMIN", "ACTIVE"));
        when(paperService.listDeleted(1, 10)).thenReturn(new PaperPageResponse(
                Arrays.asList(response(9L, "Deleted", "DELETED")),
                1L,
                1,
                10
        ));

        mockMvc.perform(get("/api/admin/papers/deleted?page=1&pageSize=10")
                        .header("Authorization", "Bearer token-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1L))
                .andExpect(jsonPath("$.items[0].status").value("DELETED"));

        verify(authService).authenticateToken("token-admin");
    }

    private static PaperResponse response(Long id, String title, String status) {
        return new PaperResponse(
                id,
                title,
                "Alice",
                "Abstract",
                2026,
                "manual",
                null,
                null,
                null,
                null,
                "keyword",
                0,
                null,
                status,
                1L,
                "2026-05-25T00:00",
                "2026-05-25T00:00"
        );
    }

    private void stubLoggedInUser(String token, UserResponse user) {
        when(authService.authenticateToken(token))
                .thenReturn(new AuthenticatedUser(user.getId(), user.getUsername(), user.getRole(), user.getStatus()));
        when(authService.currentUser()).thenReturn(user);
    }

    private static UserResponse user() {
        return new UserResponse(2L, "alice", "USER", "ACTIVE");
    }
}

