package com.doan.backend.dto.ws;

import com.doan.backend.domain.enums.UserStatus;

public record PresenceUpdateSocketRequest(
    String guildId,
    UserStatus status
) {
}
