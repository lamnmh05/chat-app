package com.doan.frontend.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class TokenStorage {
    private final Path tokenFilePath;

    public TokenStorage() {
        this.tokenFilePath = Path.of(System.getProperty("user.home"), ".chat-app-desktop", "token.txt");
    }

    public Optional<String> loadToken() {
        try {
            if (!Files.exists(tokenFilePath)) {
                return Optional.empty();
            }
            String token = Files.readString(tokenFilePath).trim();
            return token.isEmpty() ? Optional.empty() : Optional.of(token);
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    public void saveToken(String token) {
        try {
            Files.createDirectories(tokenFilePath.getParent());
            Files.writeString(tokenFilePath, token == null ? "" : token);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot save token", exception);
        }
    }

    public void clear() {
        try {
            Files.deleteIfExists(tokenFilePath);
        } catch (IOException ignored) {
        }
    }
}
