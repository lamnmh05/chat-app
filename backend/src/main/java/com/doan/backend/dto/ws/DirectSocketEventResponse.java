package com.doan.backend.dto.ws;

import com.doan.backend.domain.enums.SocketEventType;
import com.doan.backend.dto.direct.DirectMessageResponse;

public record DirectSocketEventResponse(
    SocketEventType eventType,
    DirectMessageResponse directMessage
) {
}
