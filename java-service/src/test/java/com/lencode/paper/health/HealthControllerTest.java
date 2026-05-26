package com.lencode.paper.health;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.LinkedHashMap;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.lencode.paper.auth.service.AuthService;
import com.lencode.paper.config.BearerTokenAuthenticationFilter;
import com.lencode.paper.config.JsonAccessDeniedHandler;
import com.lencode.paper.config.JsonAuthenticationEntryPoint;
import com.lencode.paper.config.SecurityConfig;

@WebMvcTest(HealthController.class)
@Import({
        SecurityConfig.class,
        BearerTokenAuthenticationFilter.class,
        JsonAuthenticationEntryPoint.class,
        JsonAccessDeniedHandler.class
})
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HealthService healthService;

    @MockBean
    private AuthService authService;

    @Test
    void returnsHealthPayload() throws Exception {
        when(healthService.check()).thenReturn(new HealthResponse(
                "UP",
                "UP",
                "UP",
                "UP",
                new LinkedHashMap<>(),
                Instant.parse("2026-05-24T00:00:00Z")
        ));

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.mysql").value("UP"))
                .andExpect(jsonPath("$.redis").value("UP"))
                .andExpect(jsonPath("$.pythonInfer").value("UP"));
    }
}

