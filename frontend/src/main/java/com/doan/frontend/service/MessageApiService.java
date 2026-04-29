package com.doan.frontend.service;

import com.doan.frontend.client.ApiClient;
import com.doan.frontend.model.message.MessageCreateRequest;
import com.doan.frontend.model.message.MessageResponse;
import com.doan.frontend.model.message.MessageUpdateRequest;
import com.doan.frontend.model.message.ReactionCreateRequest;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MessageApiService {
    private final ApiClient apiClient;

    public MessageApiService(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public CompletableFuture<List<MessageResponse>> getMessagesByChannel(String channelId) {
        return apiClient.getList("/api/channels/" + channelId + "/messages", MessageResponse[].class);
    }

    public CompletableFuture<MessageResponse> sendMessageRest(String channelId, MessageCreateRequest request) {
        return apiClient.post("/api/channels/" + channelId + "/messages", request, MessageResponse.class);
    }

    public CompletableFuture<MessageResponse> editMessage(String messageId, String content) {
        return apiClient.put("/api/messages/" + messageId, new MessageUpdateRequest(content), MessageResponse.class);
    }

    public CompletableFuture<Void> deleteMessage(String messageId) {
        return apiClient.delete("/api/messages/" + messageId);
    }

    public CompletableFuture<MessageResponse> addReaction(String messageId, String emoji) {
        return apiClient.post("/api/messages/" + messageId + "/reactions", new ReactionCreateRequest(emoji), MessageResponse.class);
    }
}
