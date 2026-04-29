package com.doan.backend.domain.document;

import com.doan.backend.domain.model.Attachment;
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
@Document("direct_messages")
public class DirectMessageDocument {
    @Id
    private String id;
    private String conversationId;
    private String senderId;
    private String receiverId;
    private String content;

    @Builder.Default
    private List<Attachment> attachments = new ArrayList<>();

    private Instant createdAt;
    private Instant editedAt;
    private boolean deleted;
}
