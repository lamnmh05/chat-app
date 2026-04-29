package com.doan.backend.service;

import com.doan.backend.domain.document.RoleDocument;
import com.doan.backend.domain.enums.Permission;
import com.doan.backend.domain.enums.RoleType;
import com.doan.backend.repository.RoleRepository;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class RoleService {
    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public List<RoleDocument> initializeDefaultRoles(String guildId) {
        RoleDocument ownerRole = RoleDocument.builder()
            .guildId(guildId)
            .name("Owner")
            .roleType(RoleType.OWNER)
            .permissions(EnumSet.allOf(Permission.class))
            .build();
        RoleDocument adminRole = RoleDocument.builder()
            .guildId(guildId)
            .name("Admin")
            .roleType(RoleType.ADMIN)
            .permissions(EnumSet.of(
                Permission.MANAGE_CHANNELS,
                Permission.DELETE_ANY_MESSAGE,
                Permission.CHAT,
                Permission.SEND_FILE,
                Permission.ADD_REACTION,
                Permission.EDIT_OWN_MESSAGE,
                Permission.DELETE_OWN_MESSAGE,
                Permission.SEND_DIRECT_MESSAGE
            ))
            .build();
        RoleDocument memberRole = RoleDocument.builder()
            .guildId(guildId)
            .name("Member")
            .roleType(RoleType.MEMBER)
            .permissions(EnumSet.of(
                Permission.CHAT,
                Permission.SEND_FILE,
                Permission.ADD_REACTION,
                Permission.EDIT_OWN_MESSAGE,
                Permission.DELETE_OWN_MESSAGE,
                Permission.SEND_DIRECT_MESSAGE
            ))
            .build();
        return roleRepository.saveAll(List.of(ownerRole, adminRole, memberRole));
    }

    public RoleDocument getRequiredRoleByType(String guildId, RoleType roleType) {
        return roleRepository.findByGuildIdAndRoleType(guildId, roleType)
            .orElseThrow(() -> new IllegalStateException("Role " + roleType + " not initialized for guild " + guildId));
    }

    public Set<RoleType> getRoleTypesByIds(List<String> roleIds) {
        return roleRepository.findAllById(roleIds)
            .stream()
            .map(RoleDocument::getRoleType)
            .collect(java.util.stream.Collectors.toSet());
    }
}
