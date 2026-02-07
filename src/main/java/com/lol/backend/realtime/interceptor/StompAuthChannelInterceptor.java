package com.lol.backend.realtime.interceptor;

import com.lol.backend.common.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * STOMP 채널 인터셉터.
 * - CONNECT: JWT 인증 → accessor.setUser(principal)
 * - SUBSCRIBE: 인증 여부 확인
 */
@Slf4j
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    public StompAuthChannelInterceptor(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (command == null) {
            return message;
        }

        switch (command) {
            case CONNECT -> handleConnect(accessor);
            case SUBSCRIBE -> handleSubscribe(accessor);
            default -> { /* no-op */ }
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.debug("STOMP CONNECT rejected: missing or invalid Authorization header");
            throw new IllegalArgumentException("인증이 필요합니다");
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        if (!jwtTokenProvider.validateToken(token)) {
            log.debug("STOMP CONNECT rejected: invalid JWT token");
            throw new IllegalArgumentException("유효하지 않은 토큰입니다");
        }

        String userId = jwtTokenProvider.getUserIdFromToken(token);
        accessor.setUser(new StompPrincipal(userId));
        log.debug("STOMP CONNECT authenticated: userId={}", userId);
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        if (user == null) {
            log.debug("STOMP SUBSCRIBE rejected: not authenticated");
            throw new IllegalArgumentException("인증이 필요합니다");
        }
        log.debug("STOMP SUBSCRIBE allowed: userId={}, destination={}", user.getName(), accessor.getDestination());
    }

    /**
     * STOMP Principal 구현.
     */
    private record StompPrincipal(String name) implements Principal {
        @Override
        public String getName() {
            return name;
        }
    }
}
