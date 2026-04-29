package com.doan.frontend.model.ws;

import java.util.List;

public record DirectMessageSocketRequest(
    String conversationId,
    String receiverId,
    String content,
    List<String> attachmentIds
) {
}
