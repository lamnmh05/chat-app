package com.doan.backend.controller;

import com.doan.backend.domain.document.StoredFileDocument;
import com.doan.backend.dto.file.FileUploadResponse;
import com.doan.backend.security.AuthenticatedUser;
import com.doan.backend.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileController {
    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FileUploadResponse uploadFile(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @RequestPart("file") MultipartFile file
    ) {
        return fileStorageService.upload(currentUser.userId(), file);
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> getFile(@PathVariable String fileId) {
        StoredFileDocument storedFile = fileStorageService.getRequiredStoredFile(fileId);
        Resource resource = fileStorageService.loadAsResource(fileId);
        MediaType mediaType = storedFile.getContentType() == null
            ? MediaType.APPLICATION_OCTET_STREAM
            : MediaType.parseMediaType(storedFile.getContentType());

        return ResponseEntity.ok()
            .contentType(mediaType)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + storedFile.getFileName() + "\"")
            .body(resource);
    }
}
