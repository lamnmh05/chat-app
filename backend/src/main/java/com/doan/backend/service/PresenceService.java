package com.doan.backend.service;

import com.doan.backend.domain.document.GuildMemberDocument;
import com.doan.backend.domain.enums.SocketEventType;
import com.doan.backend.domain.enums.UserStatus;
import com.doan.backend.dto.ws.ChannelSocketEventResponse;
import com.doan.backend.dto.ws.PresenceEventResponse;
import com.doan.backend.repository.GuildMemberRepository;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class PresenceService {
    private final Set<String> onlineUsers = ConcurrentHashMap.newKeySet();
    private final UserService userService;
    private final GuildMemberRepository guildMemberRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public PresenceService(
        UserService userService,
        GuildMemberRepository guildMemberRepository,
        SimpMessagingTemplate simpMessagingTemplate
    ) {
        this.userService = userService;
        this.guildMemberRepository = guildMemberRepository;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    public void markUserOnline(String userId) {
        onlineUsers.add(userId);
        userService.updateStatus(userId, UserStatus.ONLINE);
        broadcastToAllGuilds(userId, UserStatus.ONLINE);
    }

    public void markUserOffline(String userId) {
        onlineUsers.remove(userId);
        userService.updateStatus(userId, UserStatus.OFFLINE);
        broadcastToAllGuilds(userId, UserStatus.OFFLINE);
    }

    public void updatePresence(String userId, String guildId, UserStatus status) {
        if (status == UserStatus.ONLINE) {
            onlineUsers.add(userId);
        } else if (status == UserStatus.OFFLINE) {
            onlineUsers.remove(userId);
        }
        userService.updateStatus(userId, status);
        broadcast(guildId, userId, status);
    }

    public boolean isOnline(String userId) {
        return onlineUsers.contains(userId);
    }

    private void broadcastToAllGuilds(String userId, UserStatus status) {
        guildMemberRepository.findAllByUserId(userId)
            .stream()
            .map(GuildMemberDocument::getGuildId)
            .distinct()
            .forEach(guildId -> broadcast(guildId, userId, status));
    }

    private void broadcast(String guildId, String userId, UserStatus status) {
        simpMessagingTemplate.convertAndSend(
            "/topic/presence/" + guildId,
            new ChannelSocketEventResponse(
                SocketEventType.PRESENCE,
                null,
                null,
                new PresenceEventResponse(guildId, userId, status),
                null
            )
        );
    }
}
