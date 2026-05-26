package com.lencode.paper.auth.vo;

import com.lencode.paper.auth.entity.UserAccount;

public class UserResponse {

    private Long id;
    private String username;
    private String role;
    private String status;

    public UserResponse() {
    }

    public UserResponse(Long id, String username, String role, String status) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.status = status;
    }

    public static UserResponse from(UserAccount user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRole(), user.getStatus());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

