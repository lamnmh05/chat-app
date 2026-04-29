package com.doan.frontend.client;

import com.doan.frontend.model.file.FileUploadResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class ApiClient {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Supplier<String> tokenSupplier;
    private final String baseUrl;

    public ApiClient(ObjectMapper objectMapper, Supplier<String> tokenSupplier, String baseUrl) {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.objectMapper = objectMapper;
        this.tokenSupplier = tokenSupplier;
        this.baseUrl = baseUrl;
    }

    public <T> java.util.concurrent.CompletableFuture<T> get(String path, Class<T> responseType) {
        HttpRequest request = withAuth(HttpRequest.newBuilder(uri(path)))
            .GET()
            .header("Accept", "application/json")
            .build();
        return send(request, responseType);
    }

    public <T> java.util.concurrent.CompletableFuture<List<T>> getList(String path, Class<T[]> arrayType) {
        return get(path, arrayType).thenApply(Arrays::asList);
    }

    public <T> java.util.concurrent.CompletableFuture<T> post(String path, Object body, Class<T> responseType) {
        return sendJson("POST", path, body, responseType);
    }

    public <T> java.util.concurrent.CompletableFuture<T> put(String path, Object body, Class<T> responseType) {
        return sendJson("PUT", path, body, responseType);
    }

    public java.util.concurrent.CompletableFuture<Void> delete(String path) {
        HttpRequest request = withAuth(HttpRequest.newBuilder(uri(path)))
            .DELETE()
            .build();
        return sendVoid(request);
    }

    public java.util.concurrent.CompletableFuture<FileUploadResponse> uploadFile(String path, Path filePath) {
        String boundary = "Boundary-" + UUID.randomUUID();
        String fileName = filePath.getFileName().toString();
        String contentType;
        try {
            contentType = Files.probeContentType(filePath);
        } catch (IOException exception) {
            contentType = null;
        }
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        byte[] bodyBytes;
        try {
            byte[] fileBytes = Files.readAllBytes(filePath);
            byte[] prefix = (
                "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n" +
                "Content-Type: " + contentType + "\r\n\r\n"
            ).getBytes(StandardCharsets.UTF_8);
            byte[] suffix = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
            bodyBytes = new byte[prefix.length + fileBytes.length + suffix.length];
            System.arraycopy(prefix, 0, bodyBytes, 0, prefix.length);
            System.arraycopy(fileBytes, 0, bodyBytes, prefix.length, fileBytes.length);
            System.arraycopy(suffix, 0, bodyBytes, prefix.length + fileBytes.length, suffix.length);
        } catch (IOException exception) {
            return java.util.concurrent.CompletableFuture.failedFuture(exception);
        }

        HttpRequest request = withAuth(HttpRequest.newBuilder(uri(path)))
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .POST(HttpRequest.BodyPublishers.ofByteArray(bodyBytes))
            .build();
        return send(request, FileUploadResponse.class);
    }

    private <T> java.util.concurrent.CompletableFuture<T> sendJson(String method, String path, Object body, Class<T> responseType) {
        final String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(body);
        } catch (Exception exception) {
            return java.util.concurrent.CompletableFuture.failedFuture(exception);
        }

        HttpRequest.BodyPublisher publisher = HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8);
        HttpRequest.Builder builder = withAuth(HttpRequest.newBuilder(uri(path)))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json");

        HttpRequest request = switch (method) {
            case "POST" -> builder.POST(publisher).build();
            case "PUT" -> builder.PUT(publisher).build();
            default -> throw new IllegalArgumentException("Unsupported method: " + method);
        };
        return send(request, responseType);
    }

    private <T> java.util.concurrent.CompletableFuture<T> send(HttpRequest request, Class<T> responseType) {
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
            .thenApply(response -> parseResponse(response, responseType));
    }

    private java.util.concurrent.CompletableFuture<Void> sendVoid(HttpRequest request) {
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
            .thenApply(response -> {
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new ApiException(response.statusCode(), extractErrorMessage(response.body()));
                }
                return null;
            });
    }

    private <T> T parseResponse(HttpResponse<String> response, Class<T> responseType) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ApiException(response.statusCode(), extractErrorMessage(response.body()));
        }
        if (responseType == Void.class || response.body() == null || response.body().isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(response.body(), responseType);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot parse server response", exception);
        }
    }

    private String extractErrorMessage(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "HTTP request failed";
        }
        return responseBody;
    }

    private HttpRequest.Builder withAuth(HttpRequest.Builder builder) {
        String token = tokenSupplier.get();
        if (token != null && !token.isBlank()) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder;
    }

    private URI uri(String path) {
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return URI.create(path);
        }
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return URI.create(normalizedBase + normalizedPath);
    }
}
