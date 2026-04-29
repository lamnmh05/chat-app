package com.doan.backend.controller;

import com.doan.backend.dto.message.MessageCreateRequest;
import com.doan.backend.dto.message.MessageResponse;
import com.doan.backend.dto.message.MessageUpdateRequest;
import com.doan.backend.dto.message.ReactionCreateRequest;
import com.doan.backend.security.AuthenticatedUser;
import com.doan.backend.service.MessageService;
import com.doan.backend.service.RealtimeEventPublisher;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MessageController {
    private final MessageService messageService;
    private final RealtimeEventPublisher realtimeEventPublisher;

    public MessageController(MessageService messageService, RealtimeEventPublisher realtimeEventPublisher) {
        this.messageService = messageService;
        this.realtimeEventPublisher = realtimeEventPublisher;
    }

    @GetMapping("/channels/{channelId}/messages")
    public List<MessageResponse> getMessages(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @PathVariable String channelId
    ) {
        return messageService.getMessagesByChannel(currentUser.userId(), channelId);
    }

    @PostMapping("/channels/{channelId}/messages")
    public MessageResponse sendMessage(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @PathVariable String channelId,
        @Valid @RequestBody MessageCreateRequest request
    ) {
        MessageResponse message = messageService.createMessage(currentUser.userId(), channelId, request);
        realtimeEventPublisher.publishMessageCreated(message);
        return message;
    }

    @PutMapping("/messages/{messageId}")
    public MessageResponse editMessage(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @PathVariable String messageId,
        @Valid @RequestBody MessageUpdateRequest request
    ) {
        MessageResponse message = messageService.updateMessage(currentUser.userId(), messageId, request);
        realtimeEventPublisher.publishMessageUpdated(message);
        return message;
    }

    @DeleteMapping("/messages/{messageId}")
    public void deleteMessage(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @PathVariable String messageId
    ) {
        String channelId = messageService.getRequiredMessage(messageId).getChannelId();
        messageService.deleteMessage(currentUser.userId(), messageId);
        realtimeEventPublisher.publishMessageDeleted(channelId, messageId);
    }

    @PostMapping("/messages/{messageId}/reactions")
    public MessageResponse addReaction(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @PathVariable String messageId,
        @Valid @RequestBody ReactionCreateRequest request
    ) {
        MessageResponse message = messageService.addReaction(currentUser.userId(), messageId, request);
        realtimeEventPublisher.publishMessageUpdated(message);
        return message;
    }
}
