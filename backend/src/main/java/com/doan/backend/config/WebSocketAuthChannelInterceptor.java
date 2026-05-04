package com.doan.backend.config;

import com.doan.backend.security.AuthenticatedUser;
import com.doan.backend.security.JwtTokenProvider;
import com.doan.backend.security.UserAuthenticationToken;
import com.doan.backend.security.UserPrincipalService;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserPrincipalService userPrincipalService;

    public WebSocketAuthChannelInterceptor(
        JwtTokenProvider jwtTokenProvider,
        UserPrincipalService userPrincipalService
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userPrincipalService = userPrincipalService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);
            if (token != null && jwtTokenProvider.isValid(token)) {
                AuthenticatedUser principal = userPrincipalService.loadByUserId(jwtTokenProvider.extractUserId(token));
                accessor.setUser(new UserAuthenticationToken(principal));
            }
        }
        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        List<String> authorizationHeaders = accessor.getNativeHeader(HttpHeaders.AUTHORIZATION);
        if (authorizationHeaders != null && !authorizationHeaders.isEmpty()) {
            String value = authorizationHeaders.getFirst();
            if (value != null && value.startsWith("Bearer ")) {
                return value.substring(7);
            }
        }
        List<String> tokenHeaders = accessor.getNativeHeader("token");
        if (tokenHeaders != null && !tokenHeaders.isEmpty()) {
            return tokenHeaders.getFirst();
        }
        return null;
    }
}
