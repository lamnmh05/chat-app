package com.doan.backend.service;

import com.doan.backend.domain.document.GuildDocument;
import com.doan.backend.domain.document.GuildMemberDocument;
import com.doan.backend.domain.document.RoleDocument;
import com.doan.backend.domain.enums.Permission;
import com.doan.backend.domain.enums.RoleType;
import com.doan.backend.exception.ForbiddenException;
import com.doan.backend.exception.NotFoundException;
import com.doan.backend.repository.GuildMemberRepository;
import com.doan.backend.repository.GuildRepository;
import com.doan.backend.repository.RoleRepository;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class PermissionService {
    private final GuildMemberRepository guildMemberRepository;
    private final GuildRepository guildRepository;
    private final RoleRepository roleRepository;

    public PermissionService(
        GuildMemberRepository guildMemberRepository,
        GuildRepository guildRepository,
        RoleRepository roleRepository
    ) {
        this.guildMemberRepository = guildMemberRepository;
        this.guildRepository = guildRepository;
        this.roleRepository = roleRepository;
    }

    public GuildMemberDocument requireMembership(String guildId, String userId) {
        return guildMemberRepository.findByGuildIdAndUserId(guildId, userId)
            .orElseThrow(() -> new ForbiddenException("User is not a member of this guild"));
    }

    public GuildDocument requireGuild(String guildId) {
        return guildRepository.findById(guildId)
            .orElseThrow(() -> new NotFoundException("Guild not found"));
    }

    public Set<Permission> getPermissions(String guildId, String userId) {
        GuildDocument guild = requireGuild(guildId);
        if (guild.getOwnerId().equals(userId)) {
            return EnumSet.allOf(Permission.class);
        }

        GuildMemberDocument member = requireMembership(guildId, userId);
        List<RoleDocument> roles = roleRepository.findAllById(member.getRoleIds());
        EnumSet<Permission> permissions = EnumSet.noneOf(Permission.class);
        roles.forEach(role -> permissions.addAll(role.getPermissions()));
        return permissions;
    }

    public List<RoleType> getRoleTypes(GuildMemberDocument member) {
        return roleRepository.findAllById(member.getRoleIds())
            .stream()
            .map(RoleDocument::getRoleType)
            .distinct()
            .toList();
    }

    public void assertCanManageChannels(String guildId, String userId) {
        if (!getPermissions(guildId, userId).contains(Permission.MANAGE_CHANNELS)) {
            throw new ForbiddenException("User cannot manage channels");
        }
    }

    public void assertCanChat(String guildId, String userId) {
        if (!getPermissions(guildId, userId).contains(Permission.CHAT)) {
            throw new ForbiddenException("User cannot chat in this guild");
        }
    }

    public void assertCanSendFile(String guildId, String userId) {
        if (!getPermissions(guildId, userId).contains(Permission.SEND_FILE)) {
            throw new ForbiddenException("User cannot send files in this guild");
        }
    }

    public void assertCanReact(String guildId, String userId) {
        if (!getPermissions(guildId, userId).contains(Permission.ADD_REACTION)) {
            throw new ForbiddenException("User cannot react in this guild");
        }
    }

    public void assertCanDeleteMessage(String guildId, String userId, String senderId) {
        Set<Permission> permissions = getPermissions(guildId, userId);
        if (permissions.contains(Permission.DELETE_ANY_MESSAGE)) {
            return;
        }
        if (userId.equals(senderId) && permissions.contains(Permission.DELETE_OWN_MESSAGE)) {
            return;
        }
        throw new ForbiddenException("User cannot delete this message");
    }

    public void assertCanEditMessage(String guildId, String userId, String senderId) {
        Set<Permission> permissions = getPermissions(guildId, userId);
        if (userId.equals(senderId) && permissions.contains(Permission.EDIT_OWN_MESSAGE)) {
            return;
        }
        throw new ForbiddenException("User cannot edit this message");
    }
}
