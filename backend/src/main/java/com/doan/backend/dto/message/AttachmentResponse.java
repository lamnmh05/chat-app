package com.doan.backend.dto.message;

public record AttachmentResponse(
    String id,
    String fileName,
    String fileUrl,
    String contentType,
    long size
) {
}
