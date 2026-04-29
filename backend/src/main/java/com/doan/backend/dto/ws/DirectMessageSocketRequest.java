package com.doan.backend.dto.ws;

import java.util.List;

public record DirectMessageSocketRequest(
    String conversationId,
    String receiverId,
    String content,
    List<String> attachmentIds
) {
}
