package com.doan.backend.dto.channel;

import com.doan.backend.domain.enums.ChannelType;
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
}
