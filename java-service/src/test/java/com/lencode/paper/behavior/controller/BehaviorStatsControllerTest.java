package com.lencode.paper.behavior.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.lencode.paper.auth.dto.AuthenticatedUser;
import com.lencode.paper.auth.service.AuthService;
import com.lencode.paper.behavior.service.BehaviorStatsService;
import com.lencode.paper.behavior.vo.BehaviorEventTypeCountResponse;
import com.lencode.paper.behavior.vo.BehaviorStatsResponse;
import com.lencode.paper.behavior.vo.BehaviorTopPaperResponse;
import com.lencode.paper.config.BearerTokenAuthenticationFilter;
import com.lencode.paper.config.JsonAccessDeniedHandler;
import com.lencode.paper.config.JsonAuthenticationEntryPoint;
import com.lencode.paper.config.SecurityConfig;

@WebMvcTest(BehaviorStatsController.class)
@Import({
        SecurityConfig.class,
        BearerTokenAuthenticationFilter.class,
        JsonAuthenticationEntryPoint.class,
        JsonAccessDeniedHandler.class
})
class BehaviorStatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private BehaviorStatsService statsService;

    @Test
    void returnsGlobalStatsForAdmin() throws Exception {
        when(authService.authenticateToken("token-admin"))
                .thenReturn(new AuthenticatedUser(1L, "fjy", "ADMIN", "ACTIVE"));
        when(statsService.getGlobalStats()).thenReturn(stats());

        mockMvc.perform(get("/api/admin/behavior/stats")
                        .header("Authorization", "Bearer token-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventTotal").value(5))
                .andExpect(jsonPath("$.eventTypeCounts[0].eventType").value("PAPER_DETAIL_VIEW"))
                .andExpect(jsonPath("$.favoriteTotal").value(4))
                .andExpect(jsonPath("$.ratingUserTotal").value(2))
                .andExpect(jsonPath("$.averageRating").value(4.25))
                .andExpect(jsonPath("$.topDetailViewedPapers[0].paperId").value(9))
                .andExpect(jsonPath("$.topDownloadClickedPapers[0].total").value(2));

        verify(statsService).getGlobalStats();
    }

    @Test
    void rejectsUserRole() throws Exception {
        when(authService.authenticateToken("token-user"))
                .thenReturn(new AuthenticatedUser(2L, "alice", "USER", "ACTIVE"));

        mockMvc.perform(get("/api/admin/behavior/stats")
                        .header("Authorization", "Bearer token-user"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("无权限"));

        verifyNoInteractions(statsService);
    }

    @Test
    void rejectsMissingToken() throws Exception {
        mockMvc.perform(get("/api/admin/behavior/stats"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("未登录"));

        verifyNoInteractions(statsService);
    }

    private static BehaviorStatsResponse stats() {
        return BehaviorStatsResponse.ready(
                5L,
                Collections.singletonList(new BehaviorEventTypeCountResponse("PAPER_DETAIL_VIEW", 3L)),
                4L,
                2L,
                4.25,
                Collections.singletonList(new BehaviorTopPaperResponse(9L, "Paper A", 3L)),
                Collections.singletonList(new BehaviorTopPaperResponse(8L, "Paper B", 2L))
        );
    }
}
