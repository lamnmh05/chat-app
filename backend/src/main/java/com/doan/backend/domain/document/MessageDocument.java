package com.doan.backend.domain.document;

import com.doan.backend.domain.enums.MessageType;
import com.doan.backend.domain.model.Attachment;
import com.doan.backend.domain.model.Reaction;
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
@Document("messages")
public class MessageDocument {
    @Id
    private String id;
    private String channelId;
    private String guildId;
    private String senderId;
    private String content;
    private MessageType type;
    private String replyToMessageId;

    @Builder.Default
    private List<Attachment> attachments = new ArrayList<>();

    @Builder.Default
    private List<Reaction> reactions = new ArrayList<>();

    private Instant createdAt;
    private Instant editedAt;
    private boolean deleted;
}
