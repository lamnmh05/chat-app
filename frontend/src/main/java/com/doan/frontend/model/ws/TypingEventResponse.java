package com.doan.frontend.model.ws;

public record TypingEventResponse(
    String guildId,
    String channelId,
    String userId,
    String displayName,
    boolean typing
) {
}
