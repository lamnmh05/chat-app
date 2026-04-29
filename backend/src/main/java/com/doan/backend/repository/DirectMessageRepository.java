package com.doan.backend.repository;

import com.doan.backend.domain.document.DirectMessageDocument;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DirectMessageRepository extends MongoRepository<DirectMessageDocument, String> {
    List<DirectMessageDocument> findTop100ByConversationIdOrderByCreatedAtDesc(String conversationId);
}
