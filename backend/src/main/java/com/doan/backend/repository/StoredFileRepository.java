package com.doan.backend.repository;

import com.doan.backend.domain.document.StoredFileDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface StoredFileRepository extends MongoRepository<StoredFileDocument, String> {
}
