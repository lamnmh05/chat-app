package com.doan.backend.repository;

import com.doan.backend.domain.document.DirectConversationDocument;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DirectConversationRepository extends MongoRepository<DirectConversationDocument, String> {
    List<DirectConversationDocument> findAllByParticipantIdsContaining(String userId);
}
