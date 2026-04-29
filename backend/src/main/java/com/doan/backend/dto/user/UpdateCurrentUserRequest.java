package com.doan.backend.dto.user;

import com.doan.backend.domain.enums.UserStatus;
import jakarta.validation.constraints.Size;

public record UpdateCurrentUserRequest(
    @Size(min = 2, max = 60, message = "Display name must be between 2 and 60 characters")
    String displayName,
    String avatarUrl,
    UserStatus status
) {
}
