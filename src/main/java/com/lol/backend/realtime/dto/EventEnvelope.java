package com.lol.backend.realtime.dto;

/**
 * 서버 → 클라이언트 Event Envelope.
 * { type, data, meta } 형식.
 */
public record EventEnvelope<T>(
        EventType type,
        T data,
        EventMeta meta
) {
    public static <T> EventEnvelope<T> of(EventType type, T data) {
        return new EventEnvelope<>(type, data, EventMeta.create());
    }
}
