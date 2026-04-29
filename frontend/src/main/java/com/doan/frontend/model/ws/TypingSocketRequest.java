package com.doan.frontend.model.ws;

public record TypingSocketRequest(
    String guildId,
    String channelId,
    boolean typing
) {
}
