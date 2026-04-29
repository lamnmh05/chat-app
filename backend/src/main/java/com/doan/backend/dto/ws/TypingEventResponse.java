package com.doan.backend.dto.ws;

public record TypingEventResponse(
    String guildId,
    String channelId,
    String userId,
    String displayName,
    boolean typing
) {
}
