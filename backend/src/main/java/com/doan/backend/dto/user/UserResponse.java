package com.doan.backend.dto.user;

import com.doan.backend.domain.enums.UserStatus;
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
