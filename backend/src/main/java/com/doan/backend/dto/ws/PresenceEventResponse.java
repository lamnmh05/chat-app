package com.doan.backend.dto.ws;

import com.doan.backend.domain.enums.UserStatus;

public record PresenceEventResponse(
    String guildId,
    String userId,
    UserStatus status
) {
}
