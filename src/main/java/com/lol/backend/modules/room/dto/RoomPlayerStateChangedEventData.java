package com.lol.backend.modules.room.dto;

/**
 * ROOM_PLAYER_STATE_CHANGED 이벤트 데이터.
 * EVENTS.md 4.3 기준.
 */
public record RoomPlayerStateChangedEventData(
    String roomId,
    String userId,
    String state,
    String updatedAt
) {}
