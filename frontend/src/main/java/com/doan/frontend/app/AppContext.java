package com.doan.frontend.app;

import com.doan.frontend.client.ApiClient;
import com.doan.frontend.client.TokenStorage;
import com.doan.frontend.config.FrontendConfig;
import com.doan.frontend.model.auth.AuthResponse;
import com.doan.frontend.service.AuthApiService;
import com.doan.frontend.service.ChannelApiService;
import com.doan.frontend.service.DirectApiService;
import com.doan.frontend.service.FileApiService;
import com.doan.frontend.service.GuildApiService;
import com.doan.frontend.service.MessageApiService;
import com.doan.frontend.service.StompWebSocketService;
import com.doan.frontend.store.AuthStore;
import com.doan.frontend.store.ChannelStore;
import com.doan.frontend.store.GuildStore;
import com.doan.frontend.store.MessageStore;
import com.doan.frontend.store.PresenceStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Optional;

public class AppContext {
    private final FrontendConfig frontendConfig = FrontendConfig.load();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final TokenStorage tokenStorage = new TokenStorage();
    private final AuthStore authStore = new AuthStore();
    private final GuildStore guildStore = new GuildStore();
    private final ChannelStore channelStore = new ChannelStore();
    private final MessageStore messageStore = new MessageStore();
    private final PresenceStore presenceStore = new PresenceStore();
    private final ApiClient apiClient = new ApiClient(objectMapper, authStore::getAccessToken, frontendConfig.baseUrl());
    private final AuthApiService authApiService = new AuthApiService(apiClient);
    private final GuildApiService guildApiService = new GuildApiService(apiClient);
    private final ChannelApiService channelApiService = new ChannelApiService(apiClient);
    private final MessageApiService messageApiService = new MessageApiService(apiClient);
    private final FileApiService fileApiService = new FileApiService(apiClient);
    private final DirectApiService directApiService = new DirectApiService(apiClient);
    private final StompWebSocketService stompWebSocketService =
        new StompWebSocketService(objectMapper, authStore::getAccessToken, frontendConfig.baseUrl());

    public Optional<String> restoreToken() {
        Optional<String> token = tokenStorage.loadToken();
        token.ifPresent(authStore::setAccessToken);
        return token;
    }

    public void completeLogin(AuthResponse authResponse) {
        authStore.setAccessToken(authResponse.accessToken());
        authStore.setCurrentUser(authResponse.user());
        tokenStorage.saveToken(authResponse.accessToken());
    }

    public void clearSession() {
        stompWebSocketService.disconnect();
        tokenStorage.clear();
        authStore.clear();
        guildStore.clear();
        channelStore.clear();
        messageStore.clear();
        presenceStore.clear();
    }

    public FrontendConfig frontendConfig() {
        return frontendConfig;
    }

    public AuthStore authStore() {
        return authStore;
    }

    public GuildStore guildStore() {
        return guildStore;
    }

    public ChannelStore channelStore() {
        return channelStore;
    }

    public MessageStore messageStore() {
        return messageStore;
    }

    public PresenceStore presenceStore() {
        return presenceStore;
    }

    public AuthApiService authApiService() {
        return authApiService;
    }

    public GuildApiService guildApiService() {
        return guildApiService;
    }

    public ChannelApiService channelApiService() {
        return channelApiService;
    }

    public MessageApiService messageApiService() {
        return messageApiService;
    }

    public FileApiService fileApiService() {
        return fileApiService;
    }

    public DirectApiService directApiService() {
        return directApiService;
    }

    public StompWebSocketService stompWebSocketService() {
        return stompWebSocketService;
    }
}
