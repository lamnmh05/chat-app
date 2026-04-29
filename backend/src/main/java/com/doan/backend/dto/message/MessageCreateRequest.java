package com.doan.backend.dto.message;

import com.doan.backend.domain.enums.MessageType;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record MessageCreateRequest(
    String content,
    @NotNull(message = "Message type is required")
    MessageType type,
    String replyToMessageId,
    List<String> attachmentIds
) {
}
