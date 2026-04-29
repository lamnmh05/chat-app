package com.doan.frontend.model.message;

public record AttachmentResponse(
    String id,
    String fileName,
    String fileUrl,
    String contentType,
    long size
) {
}
