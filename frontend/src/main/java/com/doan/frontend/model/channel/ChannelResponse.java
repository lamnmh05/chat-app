package com.doan.frontend.model.channel;

import java.time.Instant;

public record ChannelResponse(
    String id,
    String guildId,
    String name,
    ChannelType type,
    Integer position,
    Instant createdAt,
    Instant updatedAt
) {
    @Override
    public String toString() {
        return "# " + name;
    }
}
