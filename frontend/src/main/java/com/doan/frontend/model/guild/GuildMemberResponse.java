package com.doan.frontend.model.guild;

import com.doan.frontend.model.user.UserStatus;
import java.time.Instant;
import java.util.List;

public record GuildMemberResponse(
    String id,
    String guildId,
    String userId,
    String username,
    String displayName,
    String avatarUrl,
    String nickname,
    List<String> roleIds,
    List<String> roleTypes,
    UserStatus status,
    Instant joinedAt
) {
}
