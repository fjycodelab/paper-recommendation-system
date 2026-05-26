package com.lencode.paper.auth.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.lencode.paper.config.BearerTokenAuthenticationFilter;
import com.lencode.paper.config.JsonAccessDeniedHandler;
import com.lencode.paper.config.JsonAuthenticationEntryPoint;
import com.lencode.paper.config.SecurityConfig;

@WebMvcTest(AdminController.class)
@Import({
        SecurityConfig.class,
        BearerTokenAuthenticationFilter.class,
        JsonAuthenticationEntryPoint.class,
        JsonAccessDeniedHandler.class
})
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Test
    void returnsOkForAdminToken() throws Exception {
        when(authService.authenticateToken("token-admin"))
                .thenReturn(new AuthenticatedUser(1L, "fjy", "ADMIN", "ACTIVE"));

        mockMvc.perform(get("/api/admin/ping")
                        .header("Authorization", "Bearer token-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));

        verify(authService).authenticateToken("token-admin");
    }

    @Test
    void mapsMissingTokenTo401() throws Exception {
        mockMvc.perform(get("/api/admin/ping"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("未登录"));
    }

    @Test
    void mapsUserRoleTo403() throws Exception {
        when(authService.authenticateToken("token-user"))
                .thenReturn(new AuthenticatedUser(2L, "alice", "USER", "ACTIVE"));

        mockMvc.perform(get("/api/admin/ping")
                        .header("Authorization", "Bearer token-user"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("无权限"));
    }
}

