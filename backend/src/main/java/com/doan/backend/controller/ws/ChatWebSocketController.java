package com.doan.backend.controller.ws;

import com.doan.backend.domain.document.UserDocument;
import com.doan.backend.domain.enums.MessageType;
import com.doan.backend.domain.enums.SocketEventType;
import com.doan.backend.dto.direct.DirectMessageResponse;
import com.doan.backend.dto.message.MessageCreateRequest;
import com.doan.backend.dto.message.MessageResponse;
import com.doan.backend.dto.ws.ChannelSocketEventResponse;
import com.doan.backend.dto.ws.ChatMessageSocketRequest;
import com.doan.backend.dto.ws.DirectMessageSocketRequest;
import com.doan.backend.dto.ws.PresenceUpdateSocketRequest;
import com.doan.backend.dto.ws.TypingEventResponse;
import com.doan.backend.dto.ws.TypingSocketRequest;
import com.doan.backend.exception.UnauthorizedException;
import com.doan.backend.service.DirectMessageService;
import com.doan.backend.service.MessageService;
import com.doan.backend.service.PresenceService;
import com.doan.backend.service.RealtimeEventPublisher;
import com.doan.backend.service.UserService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatWebSocketController {
    private final MessageService messageService;
    private final PresenceService presenceService;
    private final DirectMessageService directMessageService;
    private final UserService userService;
    private final RealtimeEventPublisher realtimeEventPublisher;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public ChatWebSocketController(
        MessageService messageService,
        PresenceService presenceService,
        DirectMessageService directMessageService,
        UserService userService,
        RealtimeEventPublisher realtimeEventPublisher,
        SimpMessagingTemplate simpMessagingTemplate
    ) {
        this.messageService = messageService;
        this.presenceService = presenceService;
        this.directMessageService = directMessageService;
        this.userService = userService;
        this.realtimeEventPublisher = realtimeEventPublisher;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessageSocketRequest request, java.security.Principal principal) {
        String currentUserId = requireUserId(principal);
        MessageResponse message = messageService.createMessage(
            currentUserId,
            request.channelId(),
            new MessageCreateRequest(
                request.content(),
                request.type() == null ? MessageType.TEXT : request.type(),
                request.replyToMessageId(),
                request.attachmentIds()
            )
        );
        realtimeEventPublisher.publishMessageCreated(message);
    }

    @MessageMapping("/chat.typing")
    public void typing(@Payload TypingSocketRequest request, java.security.Principal principal) {
        String currentUserId = requireUserId(principal);
        UserDocument currentUser = userService.getRequiredUser(currentUserId);
        simpMessagingTemplate.convertAndSend(
            "/topic/channel/" + request.channelId(),
            new ChannelSocketEventResponse(
                SocketEventType.TYPING,
                null,
                new TypingEventResponse(
                    request.guildId(),
                    request.channelId(),
                    currentUserId,
                    currentUser.getDisplayName(),
                    request.typing()
                ),
                null,
                null
            )
        );
    }

    @MessageMapping("/presence.update")
    public void updatePresence(@Payload PresenceUpdateSocketRequest request, java.security.Principal principal) {
        String currentUserId = requireUserId(principal);
        presenceService.updatePresence(currentUserId, request.guildId(), request.status());
    }

    @MessageMapping("/direct.sendMessage")
    public void sendDirectMessage(@Payload DirectMessageSocketRequest request, java.security.Principal principal) {
        String currentUserId = requireUserId(principal);
        DirectMessageResponse directMessage = directMessageService.sendDirectMessage(currentUserId, request);
        realtimeEventPublisher.publishDirectMessage(currentUserId, directMessage);
        realtimeEventPublisher.publishDirectMessage(directMessage.receiverId(), directMessage);
    }

    private String requireUserId(java.security.Principal principal) {
        if (principal == null) {
            throw new UnauthorizedException("WebSocket user is not authenticated");
        }
        return principal.getName();
    }
}
