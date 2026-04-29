package com.doan.frontend.service;

import com.doan.frontend.client.ApiClient;
import com.doan.frontend.model.file.FileUploadResponse;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class FileApiService {
    private final ApiClient apiClient;

    public FileApiService(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public CompletableFuture<FileUploadResponse> uploadFile(Path path) {
        return apiClient.uploadFile("/api/files/upload", path);
    }
}
