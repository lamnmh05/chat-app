package com.doan.backend.repository;

import com.doan.backend.domain.document.GuildMemberDocument;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GuildMemberRepository extends MongoRepository<GuildMemberDocument, String> {
    List<GuildMemberDocument> findAllByGuildId(String guildId);

    List<GuildMemberDocument> findAllByUserId(String userId);

    Optional<GuildMemberDocument> findByGuildIdAndUserId(String guildId, String userId);

    boolean existsByGuildIdAndUserId(String guildId, String userId);

    void deleteByGuildIdAndUserId(String guildId, String userId);

    void deleteAllByGuildId(String guildId);
}
