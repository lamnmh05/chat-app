package com.doan.frontend.model.auth;

import com.doan.frontend.model.user.UserResponse;

public record AuthResponse(
    String accessToken,
    String tokenType,
    UserResponse user
) {
}
