package com.doan.backend.controller;

import com.doan.backend.dto.guild.GuildCreateRequest;
import com.doan.backend.dto.guild.GuildMemberResponse;
import com.doan.backend.dto.guild.GuildResponse;
import com.doan.backend.dto.guild.GuildUpdateRequest;
import com.doan.backend.security.AuthenticatedUser;
import com.doan.backend.service.GuildService;
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
@RequestMapping("/api/guilds")
public class GuildController {
    private final GuildService guildService;

    public GuildController(GuildService guildService) {
        this.guildService = guildService;
    }

    @PostMapping
    public GuildResponse createGuild(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @Valid @RequestBody GuildCreateRequest request
    ) {
        return guildService.createGuild(currentUser.userId(), request);
    }

    @GetMapping("/my")
    public List<GuildResponse> getMyGuilds(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return guildService.getMyGuilds(currentUser.userId());
    }

    @GetMapping("/{guildId}")
    public GuildResponse getGuild(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @PathVariable String guildId
    ) {
        return guildService.getGuild(currentUser.userId(), guildId);
    }

    @PutMapping("/{guildId}")
    public GuildResponse updateGuild(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @PathVariable String guildId,
        @Valid @RequestBody GuildUpdateRequest request
    ) {
        return guildService.updateGuild(currentUser.userId(), guildId, request);
    }

    @DeleteMapping("/{guildId}")
    public void deleteGuild(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @PathVariable String guildId
    ) {
        guildService.deleteGuild(currentUser.userId(), guildId);
    }

    @PostMapping("/{guildId}/join")
    public GuildResponse joinGuild(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @PathVariable String guildId
    ) {
        return guildService.joinGuild(currentUser.userId(), guildId);
    }

    @PostMapping("/{guildId}/leave")
    public void leaveGuild(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @PathVariable String guildId
    ) {
        guildService.leaveGuild(currentUser.userId(), guildId);
    }

    @GetMapping("/{guildId}/members")
    public List<GuildMemberResponse> getMembers(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @PathVariable String guildId
    ) {
        return guildService.getGuildMembers(currentUser.userId(), guildId);
    }
}
