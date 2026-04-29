package com.doan.frontend.model.ws;

import com.doan.frontend.model.message.MessageType;
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
