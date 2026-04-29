package com.doan.backend.domain.document;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("stored_files")
public class StoredFileDocument {
    @Id
    private String id;
    private String fileName;
    private String fileUrl;
    private String contentType;
    private long size;
    private String storagePath;
    private Instant createdAt;
    private String uploadedBy;
}
