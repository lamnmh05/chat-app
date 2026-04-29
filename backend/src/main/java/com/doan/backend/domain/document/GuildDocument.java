package com.doan.backend.domain.document;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("guilds")
public class GuildDocument {
    @Id
    private String id;
    private String name;
    private String avatarUrl;
    private String ownerId;
    private Instant createdAt;
    private Instant updatedAt;
}
