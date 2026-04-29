package com.doan.backend.dto.guild;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GuildCreateRequest(
    @NotBlank(message = "Guild name is required")
    @Size(min = 2, max = 80, message = "Guild name must be between 2 and 80 characters")
    String name,
    String avatarUrl
) {
}
