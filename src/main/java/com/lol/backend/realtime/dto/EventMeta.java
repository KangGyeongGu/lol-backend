package com.lol.backend.realtime.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Event Envelope 메타데이터.
 * CONVENTIONS.md 3.1 기준.
 */
public record EventMeta(
        String eventId,
        String serverTime
) {
    public static EventMeta create() {
        return new EventMeta(
                UUID.randomUUID().toString(),
                Instant.now().toString()
        );
    }

    /**
     * 지정된 서버 시간으로 EventMeta를 생성한다.
     * remainingMs와 같은 시점의 Instant를 사용하여 SSOT 계약을 준수한다.
     *
     * @param serverTime 서버 시간 (Instant)
     * @return EventMeta
     */
    public static EventMeta create(Instant serverTime) {
        return new EventMeta(
                UUID.randomUUID().toString(),
                serverTime.toString()
        );
    }
}
