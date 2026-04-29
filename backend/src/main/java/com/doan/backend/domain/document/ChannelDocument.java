package com.doan.backend.domain.document;

import com.doan.backend.domain.enums.ChannelType;
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
@Document("channels")
public class ChannelDocument {
    @Id
    private String id;
    private String guildId;
    private String name;
    private ChannelType type;
    private Integer position;
    private Instant createdAt;
    private Instant updatedAt;
}
