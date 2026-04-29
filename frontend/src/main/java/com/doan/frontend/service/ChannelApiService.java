package com.doan.frontend.service;

import com.doan.frontend.client.ApiClient;
import com.doan.frontend.model.channel.ChannelCreateRequest;
import com.doan.frontend.model.channel.ChannelResponse;
import com.doan.frontend.model.channel.ChannelType;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ChannelApiService {
    private final ApiClient apiClient;

    public ChannelApiService(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public CompletableFuture<List<ChannelResponse>> getChannelsByGuild(String guildId) {
        return apiClient.getList("/api/guilds/" + guildId + "/channels", ChannelResponse[].class);
    }

    public CompletableFuture<ChannelResponse> createChannel(String guildId, String name) {
        return apiClient.post(
            "/api/guilds/" + guildId + "/channels",
            new ChannelCreateRequest(name, ChannelType.TEXT),
            ChannelResponse.class
        );
    }

    public CompletableFuture<Void> deleteChannel(String channelId) {
        return apiClient.delete("/api/channels/" + channelId);
    }
}
