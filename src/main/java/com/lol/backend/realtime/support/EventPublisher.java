package com.lol.backend.realtime.support;

import com.lol.backend.common.exception.ErrorCode;
import com.lol.backend.realtime.dto.EventEnvelope;
import com.lol.backend.realtime.dto.EventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * SimpMessagingTemplate 래퍼.
 * broadcast, sendToUser, sendError 편의 메서드를 제공한다.
 */
@Slf4j
@Component
public class EventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public EventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 특정 topic으로 이벤트를 브로드캐스트한다.
     */
    public <T> void broadcast(String topic, EventType type, T data) {
        EventEnvelope<T> envelope = EventEnvelope.of(type, data);
        log.debug("Broadcasting {} to {}", type, topic);
        messagingTemplate.convertAndSend(topic, envelope);
    }

    /**
     * 특정 사용자의 queue로 이벤트를 전송한다.
     */
    public <T> void sendToUser(String userId, String destination, EventType type, T data) {
        EventEnvelope<T> envelope = EventEnvelope.of(type, data);
        log.debug("Sending {} to user {} at {}", type, userId, destination);
        messagingTemplate.convertAndSendToUser(userId, destination, envelope);
    }

    /**
     * 특정 사용자에게 ERROR 이벤트를 전송한다.
     */
    public void sendError(String userId, ErrorCode errorCode, String message) {
        Map<String, Object> errorData = Map.of(
                "code", errorCode.getCode(),
                "message", message,
                "details", Map.of()
        );
        sendToUser(userId, "/queue/errors", EventType.ERROR, errorData);
    }
}
