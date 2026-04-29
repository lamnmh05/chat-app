package com.doan.backend.dto.channel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChannelUpdateRequest(
    @NotBlank(message = "Channel name is required")
    @Size(min = 2, max = 60, message = "Channel name must be between 2 and 60 characters")
    String name,
    @NotNull(message = "Position is required")
    Integer position
) {
}
