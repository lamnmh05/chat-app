package com.doan.frontend.service;

import com.doan.frontend.client.ApiClient;
import com.doan.frontend.model.direct.DirectConversationCreateRequest;
import com.doan.frontend.model.direct.DirectConversationResponse;
import com.doan.frontend.model.direct.DirectMessageResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DirectApiService {
    private final ApiClient apiClient;

    public DirectApiService(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public CompletableFuture<List<DirectConversationResponse>> getConversations() {
        return apiClient.getList("/api/direct/conversations", DirectConversationResponse[].class);
    }

    public CompletableFuture<List<DirectMessageResponse>> getMessages(String conversationId) {
        return apiClient.getList("/api/direct/conversations/" + conversationId + "/messages", DirectMessageResponse[].class);
    }

    public CompletableFuture<DirectConversationResponse> createConversation(String targetUserId) {
        return apiClient.post(
            "/api/direct/conversations",
            new DirectConversationCreateRequest(targetUserId),
            DirectConversationResponse.class
        );
    }
}
