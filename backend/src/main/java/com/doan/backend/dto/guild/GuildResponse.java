package com.doan.backend.dto.guild;

import java.time.Instant;

public record GuildResponse(
    String id,
    String name,
    String avatarUrl,
    String ownerId,
    Instant createdAt,
    Instant updatedAt
) {
}
