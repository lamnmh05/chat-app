package com.doan.backend.service;

import com.doan.backend.domain.document.GuildDocument;
import com.doan.backend.domain.document.GuildMemberDocument;
import com.doan.backend.domain.document.RoleDocument;
import com.doan.backend.domain.document.UserDocument;
import com.doan.backend.domain.enums.RoleType;
import com.doan.backend.dto.guild.GuildCreateRequest;
import com.doan.backend.dto.guild.GuildMemberResponse;
import com.doan.backend.dto.guild.GuildResponse;
import com.doan.backend.dto.guild.GuildUpdateRequest;
import com.doan.backend.exception.BadRequestException;
import com.doan.backend.exception.ForbiddenException;
import com.doan.backend.repository.ChannelRepository;
import com.doan.backend.repository.GuildMemberRepository;
import com.doan.backend.repository.GuildRepository;
import com.doan.backend.repository.MessageRepository;
import com.doan.backend.repository.RoleRepository;
import com.doan.backend.repository.UserRepository;
import com.doan.backend.support.EntityMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class GuildService {
    private final GuildRepository guildRepository;
    private final GuildMemberRepository guildMemberRepository;
    private final UserRepository userRepository;
    private final RoleService roleService;
    private final PermissionService permissionService;
    private final RoleRepository roleRepository;
    private final ChannelRepository channelRepository;
    private final MessageRepository messageRepository;

    public GuildService(
        GuildRepository guildRepository,
        GuildMemberRepository guildMemberRepository,
        UserRepository userRepository,
        RoleService roleService,
        PermissionService permissionService,
        RoleRepository roleRepository,
        ChannelRepository channelRepository,
        MessageRepository messageRepository
    ) {
        this.guildRepository = guildRepository;
        this.guildMemberRepository = guildMemberRepository;
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.permissionService = permissionService;
        this.roleRepository = roleRepository;
        this.channelRepository = channelRepository;
        this.messageRepository = messageRepository;
    }

    public GuildResponse createGuild(String currentUserId, GuildCreateRequest request) {
        Instant now = Instant.now();
        GuildDocument guild = GuildDocument.builder()
            .name(request.name().trim())
            .avatarUrl(blankToNull(request.avatarUrl()))
            .ownerId(currentUserId)
            .createdAt(now)
            .updatedAt(now)
            .build();
        GuildDocument savedGuild = guildRepository.save(guild);

        List<RoleDocument> roles = roleService.initializeDefaultRoles(savedGuild.getId());
        String ownerRoleId = roles.stream()
            .filter(role -> role.getRoleType() == RoleType.OWNER)
            .findFirst()
            .orElseThrow()
            .getId();

        guildMemberRepository.save(GuildMemberDocument.builder()
            .guildId(savedGuild.getId())
            .userId(currentUserId)
            .roleIds(new ArrayList<>(List.of(ownerRoleId)))
            .joinedAt(now)
            .build());

        return EntityMapper.toGuildResponse(savedGuild);
    }

    public List<GuildResponse> getMyGuilds(String currentUserId) {
        List<String> guildIds = guildMemberRepository.findAllByUserId(currentUserId)
            .stream()
            .map(GuildMemberDocument::getGuildId)
            .distinct()
            .toList();
        return guildRepository.findAllById(guildIds)
            .stream()
            .map(EntityMapper::toGuildResponse)
            .toList();
    }

    public GuildResponse getGuild(String currentUserId, String guildId) {
        permissionService.requireMembership(guildId, currentUserId);
        return EntityMapper.toGuildResponse(permissionService.requireGuild(guildId));
    }

    public GuildResponse updateGuild(String currentUserId, String guildId, GuildUpdateRequest request) {
        GuildDocument guild = permissionService.requireGuild(guildId);
        if (!guild.getOwnerId().equals(currentUserId)) {
            throw new ForbiddenException("Only guild owner can update guild");
        }
        guild.setName(request.name().trim());
        guild.setAvatarUrl(blankToNull(request.avatarUrl()));
        guild.setUpdatedAt(Instant.now());
        return EntityMapper.toGuildResponse(guildRepository.save(guild));
    }

    public void deleteGuild(String currentUserId, String guildId) {
        GuildDocument guild = permissionService.requireGuild(guildId);
        if (!guild.getOwnerId().equals(currentUserId)) {
            throw new ForbiddenException("Only guild owner can delete guild");
        }
        messageRepository.deleteAllByGuildId(guildId);
        channelRepository.deleteAllByGuildId(guildId);
        guildMemberRepository.deleteAllByGuildId(guildId);
        roleRepository.deleteAllByGuildId(guildId);
        guildRepository.delete(guild);
    }

    public GuildResponse joinGuild(String currentUserId, String guildId) {
        GuildDocument guild = permissionService.requireGuild(guildId);
        if (guildMemberRepository.existsByGuildIdAndUserId(guildId, currentUserId)) {
            throw new BadRequestException("User is already a member of this guild");
        }

        RoleDocument memberRole = roleService.getRequiredRoleByType(guildId, RoleType.MEMBER);
        guildMemberRepository.save(GuildMemberDocument.builder()
            .guildId(guildId)
            .userId(currentUserId)
            .roleIds(new ArrayList<>(List.of(memberRole.getId())))
            .joinedAt(Instant.now())
            .build());
        return EntityMapper.toGuildResponse(guild);
    }

    public void leaveGuild(String currentUserId, String guildId) {
        GuildDocument guild = permissionService.requireGuild(guildId);
        if (guild.getOwnerId().equals(currentUserId)) {
            throw new BadRequestException("Guild owner cannot leave their own guild");
        }
        permissionService.requireMembership(guildId, currentUserId);
        guildMemberRepository.deleteByGuildIdAndUserId(guildId, currentUserId);
    }

    public List<GuildMemberResponse> getGuildMembers(String currentUserId, String guildId) {
        permissionService.requireMembership(guildId, currentUserId);
        List<GuildMemberDocument> members = guildMemberRepository.findAllByGuildId(guildId);
        Map<String, UserDocument> userMap = userRepository.findAllById(
                members.stream().map(GuildMemberDocument::getUserId).toList()
            )
            .stream()
            .collect(Collectors.toMap(UserDocument::getId, Function.identity()));

        return members.stream()
            .map(member -> EntityMapper.toGuildMemberResponse(
                member,
                userMap.get(member.getUserId()),
                new ArrayList<>(roleService.getRoleTypesByIds(member.getRoleIds()))
            ))
            .toList();
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
