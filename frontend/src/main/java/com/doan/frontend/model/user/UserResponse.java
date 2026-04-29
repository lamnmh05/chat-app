package com.doan.frontend.model.user;

import java.time.Instant;

public record UserResponse(
    String id,
    String username,
    String email,
    String displayName,
    String avatarUrl,
    UserStatus status,
    Instant createdAt,
    Instant updatedAt
) {
}
