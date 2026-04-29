package com.doan.backend.repository;

import com.doan.backend.domain.document.RoleDocument;
import com.doan.backend.domain.enums.RoleType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RoleRepository extends MongoRepository<RoleDocument, String> {
    List<RoleDocument> findAllByGuildId(String guildId);

    Optional<RoleDocument> findByGuildIdAndRoleType(String guildId, RoleType roleType);

    void deleteAllByGuildId(String guildId);
}
