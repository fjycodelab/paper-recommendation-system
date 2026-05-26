package com.lencode.paper.config;

import java.io.IOException;
import java.util.Collections;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.lencode.paper.auth.dto.AuthenticatedUser;
import com.lencode.paper.auth.service.AuthService;
import com.lencode.paper.common.exception.UnauthorizedException;

@Component
public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ROLE_PREFIX = "ROLE_";

    private final AuthService authService;
    private final JsonAuthenticationEntryPoint authenticationEntryPoint;

    public BearerTokenAuthenticationFilter(
            AuthService authService,
            JsonAuthenticationEntryPoint authenticationEntryPoint) {
        this.authService = authService;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = extractToken(request);
            if (token != null) {
                AuthenticatedUser user = authService.authenticateToken(token);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        user,
                        token,
                        Collections.singletonList(new SimpleGrantedAuthority(toRoleAuthority(user.getRole())))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            filterChain.doFilter(request, response);
        } catch (UnauthorizedException ex) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(request, response, new BadCredentialsException(ex.getMessage(), ex));
        }
    }

    private static String extractToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!hasText(authorizationHeader)) {
            return null;
        }
        if (!authorizationHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            throw new UnauthorizedException("无效 token");
        }
        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (!hasText(token)) {
            throw new UnauthorizedException("未登录");
        }
        return token;
    }

    private static String toRoleAuthority(String role) {
        if (role != null && role.startsWith(ROLE_PREFIX)) {
            return role;
        }
        return ROLE_PREFIX + role;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
