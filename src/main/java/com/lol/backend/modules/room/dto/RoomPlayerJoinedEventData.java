package com.lol.backend.modules.room.dto;

/**
 * ROOM_PLAYER_JOINED 이벤트 데이터.
 * EVENTS.md 4.1 기준.
 */
public record RoomPlayerJoinedEventData(
    String roomId,
    String userId,
    String nickname,
    String state,
    String joinedAt
) {}
