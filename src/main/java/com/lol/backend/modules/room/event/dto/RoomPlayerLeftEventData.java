package com.lol.backend.modules.room.event.dto;

/**
 * ROOM_PLAYER_LEFT 이벤트 데이터.
 * EVENTS.md 4.2 기준.
 */
public record RoomPlayerLeftEventData(
    String roomId,
    String userId,
    String leftAt,
    String reason
) {}
