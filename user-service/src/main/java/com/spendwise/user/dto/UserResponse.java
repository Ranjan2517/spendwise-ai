package com.spendwise.user.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class UserResponse {

    private final UUID id;
    private final String email;
    private final String roles;
    private final LocalDateTime createdAt;

    public UserResponse(UUID id, String email, String roles, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.roles = roles;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getRoles() { return roles; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}