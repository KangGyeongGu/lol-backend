package com.lol.backend.modules.room.dto;

/**
 * ROOM_KICKED 이벤트 데이터.
 * EVENTS.md 4.5 기준.
 */
public record RoomKickedEventData(
    String roomId,
    String kickedByUserId,
    String kickedAt
) {}
