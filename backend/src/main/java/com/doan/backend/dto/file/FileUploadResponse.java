package com.doan.backend.dto.file;

public record FileUploadResponse(
    String id,
    String fileName,
    String fileUrl,
    String contentType,
    long size
) {
}
