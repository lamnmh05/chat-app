package com.doan.frontend.model.file;

public record FileUploadResponse(
    String id,
    String fileName,
    String fileUrl,
    String contentType,
    long size
) {
}
