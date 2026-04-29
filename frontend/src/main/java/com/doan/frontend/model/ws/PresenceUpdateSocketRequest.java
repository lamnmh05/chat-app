package com.doan.frontend.model.ws;

import com.doan.frontend.model.user.UserStatus;

public record PresenceUpdateSocketRequest(
    String guildId,
    UserStatus status
) {
}
