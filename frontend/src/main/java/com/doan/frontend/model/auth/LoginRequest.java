package com.doan.frontend.model.auth;

public record LoginRequest(
    String identifier,
    String password
) {
}
