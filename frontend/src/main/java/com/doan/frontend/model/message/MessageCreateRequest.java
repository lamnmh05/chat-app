package com.doan.frontend.model.message;

import java.util.List;

public record MessageCreateRequest(
    String content,
    MessageType type,
    String replyToMessageId,
    List<String> attachmentIds
) {
}
