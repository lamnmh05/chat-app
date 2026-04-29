package com.doan.frontend.model.direct;

import com.doan.frontend.model.message.AttachmentResponse;
import java.time.Instant;
import java.util.List;

public record DirectMessageResponse(
    String id,
    String conversationId,
    String senderId,
    String receiverId,
    String content,
    List<AttachmentResponse> attachments,
    Instant createdAt,
    Instant editedAt,
    boolean deleted
) {
}
