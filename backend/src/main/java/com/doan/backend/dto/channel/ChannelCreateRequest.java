package com.doan.backend.dto.channel;

import com.doan.backend.domain.enums.ChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChannelCreateRequest(
    @NotBlank(message = "Channel name is required")
    @Size(min = 2, max = 60, message = "Channel name must be between 2 and 60 characters")
    String name,
    @NotNull(message = "Channel type is required")
    ChannelType type
) {
}
