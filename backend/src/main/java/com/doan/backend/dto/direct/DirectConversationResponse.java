package com.doan.backend.dto.direct;

import java.time.Instant;
import java.util.List;

public record DirectConversationResponse(
    String id,
    List<String> participantIds,
    Instant createdAt
) {
}
