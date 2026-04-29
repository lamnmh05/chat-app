package com.doan.backend.dto.message;

import java.util.Set;

public record ReactionResponse(
    String emoji,
    Set<String> userIds
) {
}
