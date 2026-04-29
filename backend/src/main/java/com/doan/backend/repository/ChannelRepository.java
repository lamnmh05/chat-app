package com.doan.backend.repository;

import com.doan.backend.domain.document.ChannelDocument;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChannelRepository extends MongoRepository<ChannelDocument, String> {
    List<ChannelDocument> findAllByGuildIdOrderByPositionAscCreatedAtAsc(String guildId);

    long countByGuildId(String guildId);

    void deleteAllByGuildId(String guildId);
}
