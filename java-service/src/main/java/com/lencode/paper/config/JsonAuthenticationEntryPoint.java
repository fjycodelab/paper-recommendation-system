package com.lencode.paper.config;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.lencode.paper.common.exception.UnauthorizedException;
import com.lencode.paper.common.response.ApiError;

@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JsonAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiError.of(resolveMessage(authException)));
    }

    private static String resolveMessage(AuthenticationException authException) {
        Throwable cause = authException.getCause();
        if (cause instanceof UnauthorizedException && hasText(cause.getMessage())) {
            return cause.getMessage();
        }
        if (authException instanceof BadCredentialsException && hasText(authException.getMessage())) {
            return authException.getMessage();
        }
        return "未登录";
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
