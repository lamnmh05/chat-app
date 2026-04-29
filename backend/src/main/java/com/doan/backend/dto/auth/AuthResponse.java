package com.doan.backend.dto.auth;

import com.doan.backend.dto.user.UserResponse;

public record AuthResponse(
    String accessToken,
    String tokenType,
    UserResponse user
) {
}
