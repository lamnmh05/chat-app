package com.doan.backend.dto.message;

import jakarta.validation.constraints.NotBlank;

public record ReactionCreateRequest(
    @NotBlank(message = "Emoji is required")
    String emoji
) {
}
