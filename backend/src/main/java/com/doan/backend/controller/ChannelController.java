package com.doan.backend.controller;

import com.doan.backend.dto.channel.ChannelCreateRequest;
import com.doan.backend.dto.channel.ChannelResponse;
import com.doan.backend.dto.channel.ChannelUpdateRequest;
import com.doan.backend.security.AuthenticatedUser;
import com.doan.backend.service.ChannelService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ChannelController {
    private final ChannelService channelService;

    public ChannelController(ChannelService channelService) {
        this.channelService = channelService;
    }

    @PostMapping("/guilds/{guildId}/channels")
    public ChannelResponse createChannel(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @PathVariable String guildId,
        @Valid @RequestBody ChannelCreateRequest request
    ) {
        return channelService.createChannel(currentUser.userId(), guildId, request);
    }

    @GetMapping("/guilds/{guildId}/channels")
    public List<ChannelResponse> getChannels(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @PathVariable String guildId
    ) {
        return channelService.getChannelsByGuild(currentUser.userId(), guildId);
    }

    @PutMapping("/channels/{channelId}")
    public ChannelResponse updateChannel(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @PathVariable String channelId,
        @Valid @RequestBody ChannelUpdateRequest request
    ) {
        return channelService.updateChannel(currentUser.userId(), channelId, request);
    }

    @DeleteMapping("/channels/{channelId}")
    public void deleteChannel(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @PathVariable String channelId
    ) {
        channelService.deleteChannel(currentUser.userId(), channelId);
    }
}
