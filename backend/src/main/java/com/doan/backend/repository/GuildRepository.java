package com.doan.backend.repository;

import com.doan.backend.domain.document.GuildDocument;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GuildRepository extends MongoRepository<GuildDocument, String> {
    List<GuildDocument> findAllByOwnerId(String ownerId);
}
