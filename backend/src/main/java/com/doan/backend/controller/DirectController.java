package com.doan.backend.controller;

import com.doan.backend.dto.direct.DirectConversationCreateRequest;
import com.doan.backend.dto.direct.DirectConversationResponse;
import com.doan.backend.dto.direct.DirectMessageResponse;
import com.doan.backend.security.AuthenticatedUser;
import com.doan.backend.service.DirectMessageService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/direct")
public class DirectController {
    private final DirectMessageService directMessageService;

    public DirectController(DirectMessageService directMessageService) {
        this.directMessageService = directMessageService;
    }

    @GetMapping("/conversations")
    public List<DirectConversationResponse> getConversations(
        @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return directMessageService.getConversations(currentUser.userId());
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public List<DirectMessageResponse> getMessages(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @PathVariable String conversationId
    ) {
        return directMessageService.getMessagesByConversation(currentUser.userId(), conversationId);
    }

    @PostMapping("/conversations")
    public DirectConversationResponse createConversation(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @Valid @RequestBody DirectConversationCreateRequest request
    ) {
        return directMessageService.createConversation(currentUser.userId(), request);
    }
}
