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
}
