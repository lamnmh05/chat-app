package com.doan.frontend.model.ws;

import com.doan.frontend.model.user.UserStatus;

public record PresenceEventResponse(
    String guildId,
    String userId,
    UserStatus status
) {
}
