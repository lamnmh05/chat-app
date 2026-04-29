package com.doan.backend.dto.ws;

import com.doan.backend.domain.enums.SocketEventType;
import com.doan.backend.dto.message.MessageResponse;

public record ChannelSocketEventResponse(
    SocketEventType eventType,
    MessageResponse message,
    TypingEventResponse typing,
    PresenceEventResponse presence,
    String messageId
) {
}
