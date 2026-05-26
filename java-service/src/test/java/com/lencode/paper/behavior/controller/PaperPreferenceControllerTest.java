package com.lencode.paper.behavior.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.lencode.paper.behavior.service.PaperPreferenceService;
import com.lencode.paper.behavior.vo.UserPaperStateResponse;
import com.lencode.paper.config.BearerTokenAuthenticationFilter;
import com.lencode.paper.config.JsonAccessDeniedHandler;
import com.lencode.paper.config.JsonAuthenticationEntryPoint;
import com.lencode.paper.config.SecurityConfig;

@WebMvcTest(PaperPreferenceController.class)
@Import({
        SecurityConfig.class,
        BearerTokenAuthenticationFilter.class,
        JsonAuthenticationEntryPoint.class,
        JsonAccessDeniedHandler.class
})
class PaperPreferenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaperPreferenceService preferenceService;

    @MockBean
    private AuthService authService;

    @Test
    void favoritesPaperForLoggedInUser() throws Exception {
        UserResponse user = user();
        when(authService.authenticateToken("token-user"))
                .thenReturn(new AuthenticatedUser(7L, "alice", "USER", "ACTIVE"));
        when(authService.currentUser()).thenReturn(user);
        when(preferenceService.favorite(eq(9L), any(UserResponse.class)))
                .thenReturn(new UserPaperStateResponse(9L, true, null));

        mockMvc.perform(post("/api/papers/9/favorite")
                        .header("Authorization", "Bearer token-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paperId").value(9L))
                .andExpect(jsonPath("$.favorited").value(true));

        verify(preferenceService).favorite(eq(9L), any(UserResponse.class));
    }

    @Test
    void cancelsFavoriteForLoggedInUser() throws Exception {
        UserResponse user = user();
        when(authService.authenticateToken("token-user"))
                .thenReturn(new AuthenticatedUser(7L, "alice", "USER", "ACTIVE"));
        when(authService.currentUser()).thenReturn(user);
        when(preferenceService.cancelFavorite(eq(9L), any(UserResponse.class)))
                .thenReturn(new UserPaperStateResponse(9L, false, null));

        mockMvc.perform(delete("/api/papers/9/favorite")
                        .header("Authorization", "Bearer token-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorited").value(false));
    }

    @Test
    void ratesPaperForLoggedInUser() throws Exception {
        UserResponse user = user();
        when(authService.authenticateToken("token-user"))
                .thenReturn(new AuthenticatedUser(7L, "alice", "USER", "ACTIVE"));
        when(authService.currentUser()).thenReturn(user);
        when(preferenceService.rate(eq(9L), eq(5), any(UserResponse.class)))
                .thenReturn(new UserPaperStateResponse(9L, false, 5));

        mockMvc.perform(put("/api/papers/9/rating")
                        .header("Authorization", "Bearer token-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5));
    }

    @Test
    void rejectsFavoriteWithoutLogin() throws Exception {
        mockMvc.perform(post("/api/papers/9/favorite"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("未登录"));
    }

    private static UserResponse user() {
        return new UserResponse(7L, "alice", "USER", "ACTIVE");
    }
}
