package com.doan.frontend.model.ws;

import com.doan.frontend.model.direct.DirectMessageResponse;

public record DirectSocketEventResponse(
    SocketEventType eventType,
    DirectMessageResponse directMessage
) {
}
