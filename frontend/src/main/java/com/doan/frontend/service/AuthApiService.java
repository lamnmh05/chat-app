package com.doan.frontend.service;

import com.doan.frontend.client.ApiClient;
import com.doan.frontend.model.auth.AuthResponse;
import com.doan.frontend.model.auth.LoginRequest;
import com.doan.frontend.model.auth.RegisterRequest;
import com.doan.frontend.model.user.UserResponse;
import java.util.concurrent.CompletableFuture;

public class AuthApiService {
    private final ApiClient apiClient;

    public AuthApiService(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public CompletableFuture<AuthResponse> login(String identifier, String password) {
        return apiClient.post("/api/auth/login", new LoginRequest(identifier, password), AuthResponse.class);
    }

    public CompletableFuture<AuthResponse> register(String username, String email, String displayName, String password) {
        return apiClient.post(
            "/api/auth/register",
            new RegisterRequest(username, email, displayName, password),
            AuthResponse.class
        );
    }

    public CompletableFuture<UserResponse> getCurrentUser() {
        return apiClient.get("/api/auth/me", UserResponse.class);
    }
}
