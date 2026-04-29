package com.doan.backend.service;

import com.doan.backend.config.StorageProperties;
import com.doan.backend.domain.document.StoredFileDocument;
import com.doan.backend.domain.model.Attachment;
import com.doan.backend.dto.file.FileUploadResponse;
import com.doan.backend.exception.BadRequestException;
import com.doan.backend.exception.NotFoundException;
import com.doan.backend.repository.StoredFileRepository;
import com.doan.backend.support.EntityMapper;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {
    private final StoredFileRepository storedFileRepository;
    private final StorageProperties storageProperties;
    private Path uploadRoot;

    public FileStorageService(StoredFileRepository storedFileRepository, StorageProperties storageProperties) {
        this.storedFileRepository = storedFileRepository;
        this.storageProperties = storageProperties;
    }

    @PostConstruct
    public void init() throws IOException {
        this.uploadRoot = Path.of(storageProperties.uploadDir()).toAbsolutePath().normalize();
        Files.createDirectories(uploadRoot);
    }

    public FileUploadResponse upload(String currentUserId, MultipartFile multipartFile) {
        if (multipartFile.isEmpty()) {
            throw new BadRequestException("Uploaded file is empty");
        }

        String extension = extractExtension(multipartFile.getOriginalFilename());
        String storedName = UUID.randomUUID() + extension;
        Path targetPath = uploadRoot.resolve(storedName).normalize();
        try {
            Files.copy(multipartFile.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot store uploaded file", exception);
        }

        StoredFileDocument storedFile = StoredFileDocument.builder()
            .fileName(multipartFile.getOriginalFilename())
            .fileUrl("/api/files/" + storedName)
            .contentType(multipartFile.getContentType())
            .size(multipartFile.getSize())
            .storagePath(targetPath.toString())
            .createdAt(Instant.now())
            .uploadedBy(currentUserId)
            .build();
        storedFile.setId(storedName);
        return EntityMapper.toFileUploadResponse(storedFileRepository.save(storedFile));
    }

    public StoredFileDocument getRequiredStoredFile(String fileId) {
        return storedFileRepository.findById(fileId)
            .orElseThrow(() -> new NotFoundException("File not found"));
    }

    public Resource loadAsResource(String fileId) {
        try {
            Path filePath = Path.of(getRequiredStoredFile(fileId).getStoragePath());
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            }
        } catch (Exception exception) {
            throw new NotFoundException("File not found");
        }
        throw new NotFoundException("File not found");
    }

    public List<Attachment> resolveAttachments(List<String> attachmentIds) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return Collections.emptyList();
        }
        return storedFileRepository.findAllById(attachmentIds)
            .stream()
            .map(file -> Attachment.builder()
                .id(file.getId())
                .fileName(file.getFileName())
                .fileUrl(file.getFileUrl())
                .contentType(file.getContentType())
                .size(file.getSize())
                .build())
            .toList();
    }

    private String extractExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.'));
    }
}
