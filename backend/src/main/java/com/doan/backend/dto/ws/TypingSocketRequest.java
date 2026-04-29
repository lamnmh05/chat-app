package com.doan.backend.dto.ws;

public record TypingSocketRequest(
    String guildId,
    String channelId,
    boolean typing
) {
}
