package com.lol.backend.realtime.dto;

/**
 * TIME_SYNC 이벤트 payload (SSOT EVENTS.md 2.1 기준).
 * Topic: /user/queue/time
 *
 * @param serverTime 서버 시간 (ISO-8601 UTC, meta.serverTime과 동일)
 */
public record TimeSyncEventData(
        String serverTime
) {
}
