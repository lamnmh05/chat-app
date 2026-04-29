package com.doan.backend.dto.ws;

import com.doan.backend.domain.enums.MessageType;
import java.util.List;

public record ChatMessageSocketRequest(
    String guildId,
    String channelId,
    String content,
    MessageType type,
    String replyToMessageId,
    List<String> attachmentIds
) {
}
