package com.lol.backend.realtime.handler;

import com.lol.backend.state.store.EphemeralStateStore;
import com.lol.backend.state.dto.ConnectionHeartbeatDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * STOMP 연결/해제 이벤트 핸들러.
 * CONNECT/DISCONNECT 시 CONNECTION_HEARTBEAT를 Redis에 저장한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompEventHandler {

    private static final Duration HEARTBEAT_TTL = Duration.ofSeconds(30);

    private final EphemeralStateStore ephemeralStateStore;

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        Principal user = event.getUser();
        String userId = user != null ? user.getName() : "anonymous";

        if (!"anonymous".equals(userId)) {
            UUID userUuid = UUID.fromString(userId);
            ConnectionHeartbeatDto heartbeat = new ConnectionHeartbeatDto(
                    userUuid,
                    Instant.now(),
                    "CONNECTED"
            );
            ephemeralStateStore.saveHeartbeat(heartbeat, HEARTBEAT_TTL);
            log.info("STOMP session connected and heartbeat saved: userId={}", userId);
        } else {
            log.info("STOMP session connected: userId=anonymous (no heartbeat saved)");
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        Principal user = event.getUser();
        String userId = user != null ? user.getName() : "anonymous";

        if (!"anonymous".equals(userId)) {
            UUID userUuid = UUID.fromString(userId);
            ConnectionHeartbeatDto heartbeat = new ConnectionHeartbeatDto(
                    userUuid,
                    Instant.now(),
                    "DISCONNECTED"
            );
            ephemeralStateStore.saveHeartbeat(heartbeat, HEARTBEAT_TTL);
            log.info("STOMP session disconnected and heartbeat updated: userId={}, sessionId={}", userId, event.getSessionId());
        } else {
            log.info("STOMP session disconnected: userId=anonymous (no heartbeat saved), sessionId={}", event.getSessionId());
        }
    }
}
