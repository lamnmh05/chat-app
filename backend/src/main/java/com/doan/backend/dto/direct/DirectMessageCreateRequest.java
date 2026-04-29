package com.doan.backend.dto.direct;

import java.util.List;

public record DirectMessageCreateRequest(
    String receiverId,
    String content,
    List<String> attachmentIds
) {
}
