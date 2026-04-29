package com.doan.frontend.model.user;

public record UpdateCurrentUserRequest(
    String displayName,
    String avatarUrl,
    UserStatus status
) {
}
