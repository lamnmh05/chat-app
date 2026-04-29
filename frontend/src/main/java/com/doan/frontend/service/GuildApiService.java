package com.doan.frontend.service;

import com.doan.frontend.client.ApiClient;
import com.doan.frontend.model.guild.GuildCreateRequest;
import com.doan.frontend.model.guild.GuildMemberResponse;
import com.doan.frontend.model.guild.GuildResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GuildApiService {
    private final ApiClient apiClient;

    public GuildApiService(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public CompletableFuture<List<GuildResponse>> getMyGuilds() {
        return apiClient.getList("/api/guilds/my", GuildResponse[].class);
    }

    public CompletableFuture<GuildResponse> createGuild(String name) {
        return apiClient.post("/api/guilds", new GuildCreateRequest(name, null), GuildResponse.class);
    }

    public CompletableFuture<Void> deleteGuild(String guildId) {
        return apiClient.delete("/api/guilds/" + guildId);
    }

    public CompletableFuture<List<GuildMemberResponse>> getGuildMembers(String guildId) {
        return apiClient.getList("/api/guilds/" + guildId + "/members", GuildMemberResponse[].class);
    }
}
