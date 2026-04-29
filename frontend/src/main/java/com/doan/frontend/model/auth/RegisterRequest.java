package com.doan.frontend.model.auth;

public record RegisterRequest(
    String username,
    String email,
    String displayName,
    String password
) {
}
