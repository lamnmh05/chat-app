package com.doan.backend.domain.document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("guild_members")
@CompoundIndexes({
    @CompoundIndex(name = "guild_user_unique", def = "{'guildId': 1, 'userId': 1}", unique = true)
})
public class GuildMemberDocument {
    @Id
    private String id;
    private String guildId;
    private String userId;
    private String nickname;

    @Builder.Default
    private List<String> roleIds = new ArrayList<>();

    private Instant joinedAt;
}
