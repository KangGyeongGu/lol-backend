package com.lol.backend.realtime.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

/**
 * STOMP 연결/해제 이벤트 로깅 핸들러.
 */
@Component
public class StompEventHandler {

    private static final Logger log = LoggerFactory.getLogger(StompEventHandler.class);

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        Principal user = event.getUser();
        String userId = user != null ? user.getName() : "anonymous";
        log.info("STOMP session connected: userId={}", userId);
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        Principal user = event.getUser();
        String userId = user != null ? user.getName() : "anonymous";
        log.info("STOMP session disconnected: userId={}, sessionId={}", userId, event.getSessionId());
    }
}
