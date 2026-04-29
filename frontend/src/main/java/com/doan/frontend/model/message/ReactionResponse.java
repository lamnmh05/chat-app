package com.doan.frontend.model.message;

import java.util.Set;

public record ReactionResponse(
    String emoji,
    Set<String> userIds
) {
}
