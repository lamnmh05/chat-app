package com.doan.backend.dto.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MessageUpdateRequest(
    @NotBlank(message = "Content is required")
    @Size(max = 4000, message = "Message content must be less than or equal to 4000 characters")
    String content
) {
}
