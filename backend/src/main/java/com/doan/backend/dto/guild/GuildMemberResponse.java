package com.doan.backend.dto.guild;

import com.doan.backend.domain.enums.RoleType;
import com.doan.backend.domain.enums.UserStatus;
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
    List<RoleType> roleTypes,
    UserStatus status,
    Instant joinedAt
) {
}
