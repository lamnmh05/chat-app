package com.doan.frontend.model.ws;

import com.doan.frontend.model.message.MessageResponse;

public record ChannelSocketEventResponse(
    SocketEventType eventType,
    MessageResponse message,
    TypingEventResponse typing,
    PresenceEventResponse presence,
    String messageId
) {
}
