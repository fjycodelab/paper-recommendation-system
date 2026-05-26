package com.lencode.paper.behavior.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.lencode.paper.behavior.dto.BehaviorEventMessage;
import com.lencode.paper.behavior.dto.BehaviorEventRequest;
import com.lencode.paper.behavior.service.BehaviorTrackingService;
import com.lencode.paper.config.BearerTokenAuthenticationFilter;
import com.lencode.paper.config.JsonAccessDeniedHandler;
import com.lencode.paper.config.JsonAuthenticationEntryPoint;
import com.lencode.paper.config.SecurityConfig;

@WebMvcTest(BehaviorEventController.class)
@Import({
        SecurityConfig.class,
        BearerTokenAuthenticationFilter.class,
        JsonAuthenticationEntryPoint.class,
        JsonAccessDeniedHandler.class
})
class BehaviorEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private BehaviorTrackingService trackingService;

    @Test
    void recordsExternalUrlClickForLoggedInUser() throws Exception {
        when(authService.authenticateToken("token-user"))
                .thenReturn(new AuthenticatedUser(7L, "alice", "USER", "ACTIVE"));
        when(authService.currentUser()).thenReturn(new UserResponse(7L, "alice", "USER", "ACTIVE"));
        when(trackingService.recordExplicit(eq(7L), any(BehaviorEventRequest.class)))
                .thenReturn(message());

        mockMvc.perform(post("/api/behavior-events")
                        .header("Authorization", "Bearer token-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventType\":\"EXTERNAL_URL_CLICK\",\"paperId\":9}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("evt-1"))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void rejectsWithoutLogin() throws Exception {
        mockMvc.perform(post("/api/behavior-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventType\":\"EXTERNAL_URL_CLICK\",\"paperId\":9}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("未登录"));
    }

    private static BehaviorEventMessage message() {
        return new BehaviorEventMessage(
                "evt-1",
                7L,
                9L,
                BehaviorTrackingService.EXTERNAL_URL_CLICK,
                null,
                null,
                null,
                null,
                null,
                "2026-05-26T12:00:00"
        );
    }
}
