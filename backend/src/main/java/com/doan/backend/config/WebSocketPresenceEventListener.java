package com.doan.backend.config;

import com.doan.backend.security.AuthenticatedUser;
import com.doan.backend.security.UserAuthenticationToken;
import com.doan.backend.service.PresenceService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketPresenceEventListener {
    private final PresenceService presenceService;

    public WebSocketPresenceEventListener(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        String userId = extractUserId(StompHeaderAccessor.wrap(event.getMessage()).getUser());
        if (userId != null) {
            presenceService.markUserOnline(userId);
        }
    }

    @EventListener
    public void handleSessionDisconnected(SessionDisconnectEvent event) {
        String userId = extractUserId(StompHeaderAccessor.wrap(event.getMessage()).getUser());
        if (userId != null) {
            presenceService.markUserOffline(userId);
        }
    }

    private String extractUserId(java.security.Principal principal) {
        if (principal instanceof UserAuthenticationToken authenticationToken
            && authenticationToken.getPrincipal() instanceof AuthenticatedUser authenticatedUser) {
            return authenticatedUser.userId();
        }
        return null;
    }
}
