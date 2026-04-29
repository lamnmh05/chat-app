package com.doan.frontend.model.direct;

import java.time.Instant;
import java.util.List;

public record DirectConversationResponse(
    String id,
    List<String> participantIds,
    Instant createdAt
) {
}
