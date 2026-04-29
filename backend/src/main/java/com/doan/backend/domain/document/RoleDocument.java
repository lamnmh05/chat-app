package com.doan.backend.domain.document;

import com.doan.backend.domain.enums.Permission;
import com.doan.backend.domain.enums.RoleType;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("roles")
@CompoundIndexes({
    @CompoundIndex(name = "guild_role_type_unique", def = "{'guildId': 1, 'roleType': 1}", unique = true)
})
public class RoleDocument {
    @Id
    private String id;
    private String guildId;
    private String name;
    private RoleType roleType;

    @Builder.Default
    private Set<Permission> permissions = new LinkedHashSet<>();
}
