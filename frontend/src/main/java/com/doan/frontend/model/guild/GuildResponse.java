package com.doan.frontend.model.guild;

import java.time.Instant;

public record GuildResponse(
    String id,
    String name,
    String avatarUrl,
    String ownerId,
    Instant createdAt,
    Instant updatedAt
) {
    @Override
    public String toString() {
        return name;
    }
}
