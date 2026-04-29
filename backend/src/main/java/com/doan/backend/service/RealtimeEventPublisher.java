package com.doan.backend.service;

import com.doan.backend.domain.enums.SocketEventType;
import com.doan.backend.dto.direct.DirectMessageResponse;
import com.doan.backend.dto.message.MessageResponse;
import com.doan.backend.dto.ws.ChannelSocketEventResponse;
import com.doan.backend.dto.ws.DirectSocketEventResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class RealtimeEventPublisher {
    private final SimpMessagingTemplate simpMessagingTemplate;

    public RealtimeEventPublisher(SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    public void publishMessageCreated(MessageResponse message) {
        simpMessagingTemplate.convertAndSend(
            "/topic/channel/" + message.channelId(),
            new ChannelSocketEventResponse(SocketEventType.MESSAGE_CREATED, message, null, null, null)
        );
    }

    public void publishMessageUpdated(MessageResponse message) {
        simpMessagingTemplate.convertAndSend(
            "/topic/channel/" + message.channelId(),
            new ChannelSocketEventResponse(SocketEventType.MESSAGE_UPDATED, message, null, null, null)
        );
    }

    public void publishMessageDeleted(String channelId, String messageId) {
        simpMessagingTemplate.convertAndSend(
            "/topic/channel/" + channelId,
            new ChannelSocketEventResponse(SocketEventType.MESSAGE_DELETED, null, null, null, messageId)
        );
    }

    public void publishDirectMessage(String userId, DirectMessageResponse message) {
        simpMessagingTemplate.convertAndSendToUser(
            userId,
            "/queue/direct-messages",
            new DirectSocketEventResponse(SocketEventType.DIRECT_MESSAGE_CREATED, message)
        );
    }
}
