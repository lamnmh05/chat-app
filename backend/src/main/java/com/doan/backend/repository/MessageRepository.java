package com.doan.backend.repository;

import com.doan.backend.domain.document.MessageDocument;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MessageRepository extends MongoRepository<MessageDocument, String> {
    List<MessageDocument> findTop100ByChannelIdOrderByCreatedAtDesc(String channelId);

    void deleteAllByGuildId(String guildId);

    void deleteAllByChannelId(String channelId);
}
