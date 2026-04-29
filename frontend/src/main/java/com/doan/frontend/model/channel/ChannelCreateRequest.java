package com.doan.frontend.model.channel;

public record ChannelCreateRequest(
    String name,
    ChannelType type
) {
}
