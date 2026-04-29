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
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("direct_conversations")
public class DirectConversationDocument {
    @Id
    private String id;

    @Builder.Default
    private List<String> participantIds = new ArrayList<>();

    private Instant createdAt;
}
