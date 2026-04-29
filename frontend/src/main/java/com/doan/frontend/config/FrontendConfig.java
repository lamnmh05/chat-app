package com.doan.frontend.config;

public record FrontendConfig(
    String baseUrl
) {
    public static FrontendConfig load() {
        String systemProperty = System.getProperty("chat.api.baseUrl");
        if (systemProperty != null && !systemProperty.isBlank()) {
            return new FrontendConfig(systemProperty.trim());
        }

        String env = System.getenv("CHAT_API_BASE_URL");
        if (env != null && !env.isBlank()) {
            return new FrontendConfig(env.trim());
        }

        return new FrontendConfig("http://localhost:8080");
    }
}
