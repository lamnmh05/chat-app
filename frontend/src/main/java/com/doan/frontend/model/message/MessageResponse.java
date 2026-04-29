package com.doan.frontend.model.message;

import java.time.Instant;
import java.util.List;

public record MessageResponse(
    String id,
    String channelId,
    String guildId,
    String senderId,
    String senderDisplayName,
    String senderAvatarUrl,
    String content,
    MessageType type,
    String replyToMessageId,
    List<AttachmentResponse> attachments,
    List<ReactionResponse> reactions,
    Instant createdAt,
    Instant editedAt,
    boolean deleted
) {
}
