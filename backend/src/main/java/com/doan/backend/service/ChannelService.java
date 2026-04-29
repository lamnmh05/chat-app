package com.doan.backend.service;

import com.doan.backend.domain.document.ChannelDocument;
import com.doan.backend.domain.document.GuildDocument;
import com.doan.backend.dto.channel.ChannelCreateRequest;
import com.doan.backend.dto.channel.ChannelResponse;
import com.doan.backend.dto.channel.ChannelUpdateRequest;
import com.doan.backend.exception.NotFoundException;
import com.doan.backend.repository.ChannelRepository;
import com.doan.backend.repository.MessageRepository;
import com.doan.backend.support.EntityMapper;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ChannelService {
    private final ChannelRepository channelRepository;
    private final PermissionService permissionService;
    private final MessageRepository messageRepository;

    public ChannelService(
        ChannelRepository channelRepository,
        PermissionService permissionService,
        MessageRepository messageRepository
    ) {
        this.channelRepository = channelRepository;
        this.permissionService = permissionService;
        this.messageRepository = messageRepository;
    }

    public ChannelDocument getRequiredChannel(String channelId) {
        return channelRepository.findById(channelId)
            .orElseThrow(() -> new NotFoundException("Channel not found"));
    }

    public ChannelResponse createChannel(String currentUserId, String guildId, ChannelCreateRequest request) {
        GuildDocument guild = permissionService.requireGuild(guildId);
        permissionService.assertCanManageChannels(guildId, currentUserId);

        Instant now = Instant.now();
        ChannelDocument channel = ChannelDocument.builder()
            .guildId(guild.getId())
            .name(request.name().trim())
            .type(request.type())
            .position((int) channelRepository.countByGuildId(guildId))
            .createdAt(now)
            .updatedAt(now)
            .build();
        return EntityMapper.toChannelResponse(channelRepository.save(channel));
    }

    public List<ChannelResponse> getChannelsByGuild(String currentUserId, String guildId) {
        permissionService.requireMembership(guildId, currentUserId);
        return channelRepository.findAllByGuildIdOrderByPositionAscCreatedAtAsc(guildId)
            .stream()
            .map(EntityMapper::toChannelResponse)
            .toList();
    }

    public ChannelResponse updateChannel(String currentUserId, String channelId, ChannelUpdateRequest request) {
        ChannelDocument channel = getRequiredChannel(channelId);
        permissionService.assertCanManageChannels(channel.getGuildId(), currentUserId);
        channel.setName(request.name().trim());
        channel.setPosition(request.position());
        channel.setUpdatedAt(Instant.now());
        return EntityMapper.toChannelResponse(channelRepository.save(channel));
    }

    public void deleteChannel(String currentUserId, String channelId) {
        ChannelDocument channel = getRequiredChannel(channelId);
        permissionService.assertCanManageChannels(channel.getGuildId(), currentUserId);
        messageRepository.deleteAllByChannelId(channelId);
        channelRepository.delete(channel);
    }
}
