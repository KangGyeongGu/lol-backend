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

    /**
     * 지정된 서버 시간으로 EventEnvelope를 생성한다.
     * remainingMs와 같은 시점의 Instant를 사용하여 SSOT 계약을 준수한다.
     *
     * @param type 이벤트 타입
     * @param data 이벤트 데이터
     * @param serverTime 서버 시간 (Instant)
     * @return EventEnvelope
     */
    public static <T> EventEnvelope<T> of(EventType type, T data, java.time.Instant serverTime) {
        return new EventEnvelope<>(type, data, EventMeta.create(serverTime));
    }
}
