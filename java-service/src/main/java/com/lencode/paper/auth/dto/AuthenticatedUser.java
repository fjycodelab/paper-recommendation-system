package com.lencode.paper.auth.dto;

import com.lencode.paper.auth.entity.UserAccount;
import com.lencode.paper.auth.vo.UserResponse;

public class AuthenticatedUser {

    private final Long id;
    private final String username;
    private final String role;
    private final String status;

    public AuthenticatedUser(Long id, String username, String role, String status) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.status = status;
    }

    public static AuthenticatedUser from(UserAccount user) {
        return new AuthenticatedUser(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getStatus()
        );
    }

    public UserResponse toUserResponse() {
        return new UserResponse(id, username, role, status);
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public String getStatus() {
        return status;
    }
}
