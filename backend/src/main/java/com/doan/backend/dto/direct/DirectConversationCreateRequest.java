package com.doan.backend.dto.direct;

import jakarta.validation.constraints.NotBlank;

public record DirectConversationCreateRequest(
    @NotBlank(message = "Target user id is required")
    String targetUserId
) {
}
